package com.example.ranking.core

case class ExperimentGroupConfig(
  scoringFormula: Option[String] = None,
  filters: Option[List[FilterCondition]] = None,
  boostFactors: Option[Map[String, Double]] = None,
  maxResults: Option[Int] = None,
  description: Option[String] = None
)

case class ExperimentOverride(
  groupName: String,
  config: ExperimentGroupConfig,
  appliedOverrides: Set[String]
)

object ExperimentResolver {
  
  def resolveConfig(
    baseConfig: ResolvedRankingConfig,
    context: RankingContext
  ): Either[String, (ResolvedRankingConfig, Option[ExperimentOverride])] = {
    
    context.experimentGroup match {
      case None => 
        Right((baseConfig, None))
        
      case Some(experimentGroup) =>
        baseConfig.experimentGroups.get(experimentGroup) match {
          case None =>
            // Experiment group not found, use base config
            Right((baseConfig, None))
            
          case Some(experimentConfig) =>
            applyExperimentOverrides(baseConfig, experimentGroup, experimentConfig)
        }
    }
  }
  
  private def applyExperimentOverrides(
    baseConfig: ResolvedRankingConfig,
    groupName: String,
    experimentConfig: ExperimentGroupConfig
  ): Either[String, (ResolvedRankingConfig, Option[ExperimentOverride])] = {
    
    var appliedOverrides = Set.empty[String]
    
    // Apply scoring formula override if present
    val resolvedFormula = experimentConfig.scoringFormula match {
      case Some(formula) =>
        // Validate the experiment formula
        ExpressionEvaluator.validateExpression(formula) match {
          case Left(error) => return Left(s"Invalid experiment formula for group '$groupName': $error")
          case Right(_) =>
            appliedOverrides += "scoringFormula"
            formula
        }
      case None => baseConfig.scoringFormula
    }
    
    // Apply filters override if present
    val resolvedFilters = experimentConfig.filters match {
      case Some(filters) =>
        appliedOverrides += "filters"
        filters
      case None => baseConfig.filters
    }
    
    // Apply boost factors override if present
    val resolvedBoostFactors = experimentConfig.boostFactors match {
      case Some(boosts) =>
        appliedOverrides += "boostFactors"
        // Merge with base boost factors, experiment overrides take precedence
        baseConfig.boostFactors ++ boosts
      case None => baseConfig.boostFactors
    }
    
    // Apply max results override if present
    val resolvedMaxResults = experimentConfig.maxResults match {
      case Some(maxResults) =>
        appliedOverrides += "maxResults"
        maxResults
      case None => baseConfig.maxResults
    }
    
    val overriddenConfig = baseConfig.copy(
      scoringFormula = resolvedFormula,
      filters = resolvedFilters,
      boostFactors = resolvedBoostFactors,
      maxResults = resolvedMaxResults
    )
    
    val experimentOverride = if (appliedOverrides.nonEmpty) {
      Some(ExperimentOverride(groupName, experimentConfig, appliedOverrides))
    } else {
      None
    }
    
    Right((overriddenConfig, experimentOverride))
  }
}