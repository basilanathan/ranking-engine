package com.example.ranking.tests

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import com.example.ranking.core._
import com.example.ranking.config._

class ExpressionRankingIntegrationSpec extends AnyFlatSpec with Matchers {

  "Expression-based ranking" should "work end-to-end with config" in {
    val yaml = """
      |ranking_strategy: "expression_based"
      |scoring_formula: "0.6 * revenue + 0.4 * engagement - 0.1 * risk_score"
      |boost_factors:
      |  premium: 1.2
      |  trending: 1.1
      |experiment_groups:
      |  high_value:
      |    scoring_formula: "0.8 * revenue + 0.2 * engagement"
      |    boost_factors:
      |      premium: 1.5
      |""".stripMargin

    val configResult = RankingConfig.fromYamlSchema(yaml)
    configResult shouldBe a [Right[_, _]]
    
    val config = configResult.toOption.get
    val resolved = ConfigResolver.resolve(config)
    resolved shouldBe a [Right[_, _]]
  }

  it should "rank items using expression evaluation" in {
    val items = List(
      Item("offer1", 1.0, Map("revenue" -> "2.5", "engagement" -> "0.8", "risk_score" -> "0.2")),
      Item("offer2", 1.0, Map("revenue" -> "1.0", "engagement" -> "1.2", "risk_score" -> "0.5")),
      Item("offer3", 1.0, Map("revenue" -> "3.0", "engagement" -> "0.3", "risk_score" -> "0.1"))
    )

    val formula = "0.6 * revenue + 0.4 * engagement - 0.1 * risk_score"
    val strategyResult = ExpressionBasedRankingStrategy.fromConfig(formula)
    strategyResult shouldBe a [Right[_, _]]
    
    val strategy = strategyResult.toOption.get
    val rankedItems = strategy.rank(items)
    
    rankedItems should have size 3
    // Verify ranking order based on formula calculation
    val scores = items.map { item =>
      val features = item.metadata.view.mapValues(_.toDouble).toMap + ("base_score" -> item.score)
      val revenue = features("revenue")
      val engagement = features("engagement") 
      val riskScore = features("risk_score")
      
      item.id -> (0.6 * revenue + 0.4 * engagement - 0.1 * riskScore)
    }.toMap
    
    // Items should be ranked by their computed scores
    val expectedOrder = items.sortBy(item => -scores(item.id)).map(_.id)
    rankedItems.map(_.id) shouldBe expectedOrder
  }

  it should "handle missing features gracefully" in {
    val items = List(
      Item("complete", 1.0, Map("revenue" -> "2.0", "engagement" -> "1.0")),
      Item("incomplete", 1.0, Map("revenue" -> "1.5")) // missing engagement
    )

    val strategy = ExpressionBasedRankingStrategy.fromConfig("revenue + engagement").toOption.get
    val results = strategy.rankWithDetails(items)
    
    results should have size 2
    results(0) shouldBe a [Right[_, _]] // complete item should succeed
    results(1) shouldBe a [Left[_, _]]  // incomplete item should fail
  }

  it should "apply boost factors correctly" in {
    val items = List(
      Item("boosted", 1.0, Map("revenue" -> "1.0", "boosted" -> "true")),
      Item("normal", 1.0, Map("revenue" -> "1.0"))
    )

    val boosts = Map("premium" -> 1.5)
    val strategy = ExpressionBasedRankingStrategy.fromConfig("revenue", boosts).toOption.get
    val results = strategy.rankWithDetails(items)
    
    results.foreach { result =>
      result shouldBe a [Right[_, _]]
      val scored = result.toOption.get
      if (scored.item.id == "boosted") {
        scored.score should be > scored.formulaScore // Should have boost applied
      }
    }
  }

  it should "validate formulas in config" in {
    val validFormula = "revenue + engagement"
    val invalidFormula = "revenue +"
    
    ExpressionBasedRankingStrategy.fromConfig(validFormula) shouldBe a [Right[_, _]]
    ExpressionBasedRankingStrategy.fromConfig(invalidFormula) shouldBe a [Left[_, _]]
  }

  it should "work with experiment group overrides" in {
    val baseConfig = RankingConfigSchema(
      rankingStrategy = "expression_based",
      scoringFormula = "revenue + engagement",
      experimentGroups = Some(Map(
        "mobile" -> ExperimentGroupConfig(
          scoringFormula = Some("2 * revenue + engagement")
        )
      ))
    )

    val baseResolved = ConfigResolver.resolve(baseConfig)
    val mobileResolved = ConfigResolver.resolve(baseConfig, Some("mobile"))
    
    baseResolved shouldBe a [Right[_, _]]
    mobileResolved shouldBe a [Right[_, _]]
    
    baseResolved.toOption.get.scoringFormula shouldBe "revenue + engagement"
    mobileResolved.toOption.get.scoringFormula shouldBe "2 * revenue + engagement"
  }

  "Real-world example" should "demonstrate complete ranking pipeline" in {
    val offers = List(
      Item("premium-offer", 1.0, Map(
        "revenue" -> "10.5",
        "engagement" -> "0.9", 
        "risk_score" -> "0.1",
        "category" -> "finance",
        "boosted" -> "true"
      )),
      Item("standard-offer", 1.0, Map(
        "revenue" -> "5.2",
        "engagement" -> "1.3",
        "risk_score" -> "0.3",
        "category" -> "shopping"
      )),
      Item("low-value-offer", 1.0, Map(
        "revenue" -> "2.1",
        "engagement" -> "0.4",
        "risk_score" -> "0.6",
        "category" -> "entertainment"
      ))
    )

    // Complex formula with multiple factors
    val formula = "0.5 * revenue + 0.3 * engagement - 0.2 * risk_score + max(0, revenue - 5) * 0.1"
    val boosts = Map("premium" -> 1.2, "category_bonus" -> 1.1)
    
    val strategy = ExpressionBasedRankingStrategy.fromConfig(formula, boosts).toOption.get
    val rankedOffers = strategy.rank(offers)
    
    rankedOffers should have size 3
    rankedOffers.head.id should not be "low-value-offer" // Should not be first
    
    val detailedResults = strategy.rankWithDetails(offers)
    detailedResults.foreach { result =>
      result shouldBe a [Right[_, _]]
      val scored = result.toOption.get
      scored.formulaScore should be > 0.0
      scored.score should be >= scored.formulaScore // Boosts should not decrease score
    }
  }
}