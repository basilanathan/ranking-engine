error id: file://<WORKSPACE>/ranking-config/src/main/scala/com/example/ranking/config/ConfigValidator.scala:`<none>`.
file://<WORKSPACE>/ranking-config/src/main/scala/com/example/ranking/config/ConfigValidator.scala
empty definition using pc, found symbol in pc: `<none>`.
empty definition using semanticdb
empty definition using fallback
non-local guesses:
	 -RankingConfig#
	 -scala/Predef.RankingConfig#
offset: 453
uri: file://<WORKSPACE>/ranking-config/src/main/scala/com/example/ranking/config/ConfigValidator.scala
text:
```scala
package com.example.ranking.config

sealed trait ValidationError extends Product with Serializable
case class InvalidStrategy(strategy: String) extends ValidationError
case class InvalidWeight(key: String, value: Double) extends ValidationError
case class MissingRequiredParameter(parameter: String) extends ValidationError

object ConfigValidator {
  val supportedStrategies = Set("score-based", "weighted", "custom")
  
  def validate(config: RankingC@@onfig): Either[List[ValidationError], RankingConfig] = {
    val errors = validateStrategy(config) ++ validateWeights(config) ++ validateParameters(config)
    
    if (errors.isEmpty) Right(config)
    else Left(errors)
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
```


#### Short summary: 

empty definition using pc, found symbol in pc: `<none>`.