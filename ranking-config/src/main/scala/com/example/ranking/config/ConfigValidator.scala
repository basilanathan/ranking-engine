package com.example.ranking.config

sealed trait ValidationError extends Product with Serializable
case class InvalidStrategy(strategy: String) extends ValidationError
case class InvalidWeight(key: String, value: Double) extends ValidationError
case class MissingRequiredParameter(parameter: String) extends ValidationError

object ConfigValidator {
  val supportedStrategies = Set("score-based", "weighted", "custom", "revenue_optimized", "engagement_first", "simple_score")
  
  @deprecated("Use ConfigResolver.resolve instead", "0.2.0")
  def validate(config: RankingConfig): Either[List[ValidationError], RankingConfig] = {
    val errors = validateStrategy(config) ++ validateWeights(config) ++ validateParameters(config)
    
    if (errors.isEmpty) Right(config)
    else Left(errors)
  }
  
  def validateSchema(config: RankingConfigSchema): Either[List[ConfigValidationError], RankingConfigSchema] = {
    ConfigResolver.resolve(config) match {
      case Left(errors) => Left(errors)
      case Right(_) => Right(config)
    }
  }
  
  private def validateStrategy(config: RankingConfig): List[ValidationError] = {
    if (supportedStrategies.contains(config.strategy)) Nil
    else List(InvalidStrategy(config.strategy))
  }
  
  private def validateWeights(config: RankingConfig): List[ValidationError] = {
    config.weights.collect {
      case (key, value) if value < 0.0 || value > 1.0 => InvalidWeight(key, value)
    }.toList
  }
  
  private def validateParameters(config: RankingConfig): List[ValidationError] = {
    config.strategy match {
      case "weighted" if config.weights.isEmpty => 
        List(MissingRequiredParameter("weights"))
      case _ => Nil
    }
  }
}