package com.example.ranking.config

import io.circe._
import io.circe.generic.semiauto._

case class FilterCondition(
  condition: String,
  description: Option[String] = None
)

case class ExperimentGroupConfig(
  rankingStrategy: Option[String] = None,
  scoringFormula: Option[String] = None,
  filters: Option[List[FilterCondition]] = None,
  maxResults: Option[Int] = None,
  boostFactors: Option[Map[String, Double]] = None
)

case class FallbackConfig(
  rankingStrategy: String,
  scoringFormula: String,
  maxResults: Option[Int] = None
)

case class RankingConfigSchema(
  rankingStrategy: String,
  scoringFormula: String,
  filters: Option[List[FilterCondition]] = None,
  experimentGroups: Option[Map[String, ExperimentGroupConfig]] = None,
  maxResults: Option[Int] = None,
  boostFactors: Option[Map[String, Double]] = None,
  fallback: Option[FallbackConfig] = None
)

object RankingConfigSchema {
  implicit val filterConditionDecoder: Decoder[FilterCondition] = deriveDecoder[FilterCondition]
  implicit val filterConditionEncoder: Encoder[FilterCondition] = deriveEncoder[FilterCondition]
  
  implicit val experimentGroupConfigDecoder: Decoder[ExperimentGroupConfig] = Decoder.forProduct5(
    "ranking_strategy", "scoring_formula", "filters", "max_results", "boost_factors"
  )(ExperimentGroupConfig.apply)
  
  implicit val experimentGroupConfigEncoder: Encoder[ExperimentGroupConfig] = Encoder.forProduct5(
    "ranking_strategy", "scoring_formula", "filters", "max_results", "boost_factors"
  )(config => (config.rankingStrategy, config.scoringFormula, config.filters, config.maxResults, config.boostFactors))
  
  implicit val fallbackConfigDecoder: Decoder[FallbackConfig] = Decoder.forProduct3(
    "ranking_strategy", "scoring_formula", "max_results"
  )(FallbackConfig.apply)
  
  implicit val fallbackConfigEncoder: Encoder[FallbackConfig] = Encoder.forProduct3(
    "ranking_strategy", "scoring_formula", "max_results"
  )(config => (config.rankingStrategy, config.scoringFormula, config.maxResults))
  
  implicit val rankingConfigSchemaDecoder: Decoder[RankingConfigSchema] = Decoder.forProduct7(
    "ranking_strategy", "scoring_formula", "filters", "experiment_groups", "max_results", "boost_factors", "fallback"
  )(RankingConfigSchema.apply)
  
  implicit val rankingConfigSchemaEncoder: Encoder[RankingConfigSchema] = Encoder.forProduct7(
    "ranking_strategy", "scoring_formula", "filters", "experiment_groups", "max_results", "boost_factors", "fallback"
  )(config => (config.rankingStrategy, config.scoringFormula, config.filters, config.experimentGroups, config.maxResults, config.boostFactors, config.fallback))
}

sealed trait ConfigValidationError extends Product with Serializable
case class InvalidScoringFormula(formula: String, reason: String) extends ConfigValidationError
case class InvalidFilterCondition(condition: String, reason: String) extends ConfigValidationError
case class InvalidBoostFactor(key: String, value: Double, reason: String) extends ConfigValidationError
case class MissingRequiredField(field: String) extends ConfigValidationError
case class InvalidExperimentGroup(groupName: String, error: String) extends ConfigValidationError

case class ResolvedRankingConfig(
  rankingStrategy: String,
  scoringFormula: String,
  filters: List[FilterCondition],
  maxResults: Int,
  boostFactors: Map[String, Double]
)

object ConfigResolver {
  def resolve(
    baseConfig: RankingConfigSchema, 
    experimentGroup: Option[String] = None
  ): Either[List[ConfigValidationError], ResolvedRankingConfig] = {
    
    val effectiveConfig = experimentGroup match {
      case Some(groupName) => 
        baseConfig.experimentGroups
          .flatMap(_.get(groupName))
          .map(group => mergeWithExperimentGroup(baseConfig, group))
          .getOrElse(baseConfig)
      case None => baseConfig
    }
    
    val errors = validateConfig(effectiveConfig)
    if (errors.nonEmpty) {
      Left(errors)
    } else {
      Right(ResolvedRankingConfig(
        rankingStrategy = effectiveConfig.rankingStrategy,
        scoringFormula = effectiveConfig.scoringFormula,
        filters = effectiveConfig.filters.getOrElse(List.empty),
        maxResults = effectiveConfig.maxResults.getOrElse(100),
        boostFactors = effectiveConfig.boostFactors.getOrElse(Map.empty)
      ))
    }
  }
  
  private def mergeWithExperimentGroup(
    base: RankingConfigSchema, 
    group: ExperimentGroupConfig
  ): RankingConfigSchema = {
    base.copy(
      rankingStrategy = group.rankingStrategy.getOrElse(base.rankingStrategy),
      scoringFormula = group.scoringFormula.getOrElse(base.scoringFormula),
      filters = group.filters.orElse(base.filters),
      maxResults = group.maxResults.orElse(base.maxResults),
      boostFactors = group.boostFactors.orElse(base.boostFactors)
    )
  }
  
  private def validateConfig(config: RankingConfigSchema): List[ConfigValidationError] = {
    val formulaErrors = validateScoringFormula(config.scoringFormula)
    val filterErrors = config.filters.toList.flatten.flatMap(validateFilterCondition)
    val boostErrors = config.boostFactors.toList.flatten.flatMap { case (k, v) => 
      validateBoostFactor(k, v) 
    }
    
    formulaErrors ++ filterErrors ++ boostErrors
  }
  
  private def validateScoringFormula(formula: String): List[ConfigValidationError] = {
    import com.example.ranking.core.ExpressionEvaluator
    
    if (formula.trim.isEmpty) {
      List(InvalidScoringFormula(formula, "Formula cannot be empty"))
    } else {
      ExpressionEvaluator.validateExpression(formula) match {
        case Left(error) => List(InvalidScoringFormula(formula, s"Invalid syntax: $error"))
        case Right(_) => 
          ExpressionEvaluator.extractVariables(formula) match {
            case Left(error) => List(InvalidScoringFormula(formula, s"Failed to extract variables: $error"))
            case Right(vars) if vars.isEmpty => 
              List(InvalidScoringFormula(formula, "Formula must contain at least one variable"))
            case Right(_) => List.empty
          }
      }
    }
  }
  
  private def validateFilterCondition(filter: FilterCondition): List[ConfigValidationError] = {
    val condition = filter.condition.trim
    if (condition.isEmpty) {
      List(InvalidFilterCondition(condition, "Condition cannot be empty"))
    } else if (!condition.contains("==") && !condition.contains("!=") && 
               !condition.contains(">=") && !condition.contains("<=") && 
               !condition.contains(">") && !condition.contains("<")) {
      List(InvalidFilterCondition(condition, "Condition must contain a comparison operator"))
    } else {
      List.empty
    }
  }
  
  private def validateBoostFactor(key: String, value: Double): List[ConfigValidationError] = {
    if (value <= 0.0) {
      List(InvalidBoostFactor(key, value, "Boost factor must be positive"))
    } else if (value > 10.0) {
      List(InvalidBoostFactor(key, value, "Boost factor too high (max 10.0)"))
    } else {
      List.empty
    }
  }
}