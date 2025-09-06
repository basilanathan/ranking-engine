package com.example.ranking.core

case class ScoredItem(
  item: Item,
  score: Double,
  formulaScore: Double,
  appliedBoosts: Map[String, Double] = Map.empty
)

class ExpressionBasedRankingStrategy(
  scoringFormula: String,
  boostFactors: Map[String, Double] = Map.empty,
  defaultFeatures: Map[String, Double] = Map.empty
) extends RankingStrategy[Item] {
  
  def rank(items: List[Item]): List[Item] = {
    val scoredItems = items.flatMap { item =>
      scoreItem(item) match {
        case Right(scored) => Some(scored)
        case Left(_) => None // Skip items that can't be scored
      }
    }
    
    scoredItems
      .sortBy(-_.score)
      .map(_.item)
  }
  
  def rankWithDetails(items: List[Item]): List[Either[String, ScoredItem]] = {
    items.map(scoreItem)
  }
  
  private def scoreItem(item: Item): Either[String, ScoredItem] = {
    val features = combineFeatures(item)
    
    ExpressionEvaluator.evaluate(scoringFormula, features).map { formulaScore =>
      val appliedBoosts = calculateBoosts(item, features)
      val finalScore = applyBoosts(formulaScore, appliedBoosts)
      
      ScoredItem(
        item = item.copy(score = finalScore),
        score = finalScore,
        formulaScore = formulaScore,
        appliedBoosts = appliedBoosts
      )
    }
  }
  
  private def combineFeatures(item: Item): Map[String, Double] = {
    val itemFeatures = item.metadata.view.mapValues(_.toDoubleOption.getOrElse(0.0)).toMap
    val baseFeatures = Map("base_score" -> item.score)
    
    defaultFeatures ++ baseFeatures ++ itemFeatures
  }
  
  private def calculateBoosts(item: Item, features: Map[String, Double]): Map[String, Double] = {
    boostFactors.view.mapValues { factor =>
      if (shouldApplyBoost(item, features)) factor else 1.0
    }.toMap
  }
  
  private def shouldApplyBoost(item: Item, features: Map[String, Double]): Boolean = {
    // Simple heuristic - could be made configurable
    features.get("boost_eligible").exists(_ > 0.5) ||
    item.metadata.contains("boosted") ||
    item.metadata.get("priority").exists(_.toIntOption.exists(_ > 5))
  }
  
  private def applyBoosts(baseScore: Double, boosts: Map[String, Double]): Double = {
    boosts.values.foldLeft(baseScore)(_ * _)
  }
}

object ExpressionBasedRankingStrategy {
  def fromConfig(
    formula: String,
    boosts: Map[String, Double] = Map.empty,
    defaults: Map[String, Double] = Map.empty
  ): Either[String, ExpressionBasedRankingStrategy] = {
    
    ExpressionEvaluator.validateExpression(formula).map { _ =>
      new ExpressionBasedRankingStrategy(formula, boosts, defaults)
    }
  }
  
  def validateFormula(formula: String, sampleFeatures: Map[String, Double]): Either[String, Double] = {
    ExpressionEvaluator.evaluate(formula, sampleFeatures)
  }
}