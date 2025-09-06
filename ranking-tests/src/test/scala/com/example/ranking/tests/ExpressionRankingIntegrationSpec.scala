package com.example.ranking.tests

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import com.example.ranking.core._

class ExpressionRankingIntegrationSpec extends AnyFlatSpec with Matchers {

  "Expression-based ranking integration" should "work end-to-end with ResolvedRankingConfig" in {
    val offers = List(
      Offer("offer1", Map("revenue" -> 2.5, "engagement" -> 0.8, "risk_score" -> 0.2)),
      Offer("offer2", Map("revenue" -> 1.0, "engagement" -> 1.2, "risk_score" -> 0.5)),
      Offer("offer3", Map("revenue" -> 3.0, "engagement" -> 0.3, "risk_score" -> 0.1))
    )

    val config = ResolvedRankingConfig(
      rankingStrategy = "expression_based",
      scoringFormula = "0.6 * revenue + 0.4 * engagement - 0.1 * risk_score",
      filters = List.empty,
      maxResults = 10,
      boostFactors = Map("premium" -> 1.2),
      experimentGroups = Map.empty
    )

    val ranker = Ranker.fromConfig(config)
    val result = ranker.rank(offers)

    result shouldBe a[Right[_, _]]
    val rankingResult = result.toOption.get

    rankingResult.rankedOffers should have size 3
    rankingResult.totalProcessed shouldBe 3
    rankingResult.totalFiltered shouldBe 0
  }

  it should "handle experiment group overrides" in {
    val offers = List(
      Offer("offer1", Map("revenue" -> 2.0, "engagement" -> 1.0)),
      Offer("offer2", Map("revenue" -> 1.0, "engagement" -> 2.0))
    )

    val config = ResolvedRankingConfig(
      rankingStrategy = "expression_based",
      scoringFormula = "revenue + engagement", // Base: equal weight
      filters = List.empty,
      maxResults = 10,
      boostFactors = Map.empty,
      experimentGroups = Map(
        "revenue_focus" -> ExperimentGroupConfig(
          scoringFormula = Some("2 * revenue + engagement"), // Emphasize revenue
          description = Some("Revenue-focused experiment")
        )
      )
    )

    val ranker = Ranker.fromConfig(config)
    
    // Test base formula
    val baseResult = ranker.rank(offers, RankingContext.empty)
    baseResult shouldBe a[Right[_, _]]

    // Test experiment override
    val experimentContext = RankingContext.withExperiment("revenue_focus")
    val experimentResult = ranker.rank(offers, experimentContext)
    experimentResult shouldBe a[Right[_, _]]

    val baseRanking = baseResult.toOption.get
    val experimentRanking = experimentResult.toOption.get

    // Both should rank the same offers, but potentially in different order
    baseRanking.rankedOffers should have size 2
    experimentRanking.rankedOffers should have size 2

    // Experiment should apply override
    experimentRanking.experimentOverride should be(defined)
    experimentRanking.experimentOverride.get.groupName shouldBe "revenue_focus"
  }

  it should "apply boost factors correctly" in {
    val offers = List(
      Offer("premium-offer", 
        Map("revenue" -> 1.0), 
        Map("tier" -> "premium")
      ),
      Offer("normal-offer", 
        Map("revenue" -> 1.0), 
        Map("tier" -> "standard")
      )
    )

    val config = ResolvedRankingConfig(
      rankingStrategy = "expression_based",
      scoringFormula = "revenue",
      filters = List.empty,
      maxResults = 10,
      boostFactors = Map("premium" -> 1.5),
      experimentGroups = Map.empty
    )

    val ranker = Ranker.fromConfig(config)
    val result = ranker.rankWithDetails(offers, RankingContext.empty, includeDebugInfo = true)

    result shouldBe a[Right[_, _]]
    val ranking = result.toOption.get

    ranking.rankedOffers should have size 2
    
    val premiumOffer = ranking.rankedOffers.find(_.offer.id == "premium-offer").get
    val normalOffer = ranking.rankedOffers.find(_.offer.id == "normal-offer").get

    // Premium offer should have a boost applied
    premiumOffer.score should be > normalOffer.score
    premiumOffer.appliedBoosts should contain("premium" -> 1.5)
  }

  it should "validate formulas and reject invalid ones" in {
    val validConfig = ResolvedRankingConfig(
      rankingStrategy = "expression_based",
      scoringFormula = "revenue + engagement",
      filters = List.empty,
      maxResults = 10,
      boostFactors = Map.empty,
      experimentGroups = Map.empty
    )

    val invalidConfig = ResolvedRankingConfig(
      rankingStrategy = "expression_based",
      scoringFormula = "revenue +", // Invalid formula
      filters = List.empty,
      maxResults = 10,
      boostFactors = Map.empty,
      experimentGroups = Map.empty
    )

    val offers = List(Offer("test", Map("revenue" -> 1.0, "engagement" -> 0.5)))

    val validRanker = Ranker.fromConfig(validConfig)
    val validResult = validRanker.rank(offers)
    validResult shouldBe a[Right[_, _]]

    val invalidRanker = Ranker.fromConfig(invalidConfig)
    val invalidResult = invalidRanker.rank(offers)
    invalidResult shouldBe a[Left[_, _]]
  }

  "Real-world ranking example" should "demonstrate complete ranking pipeline" in {
    val offers = List(
      Offer("premium-finance", 
        Map("revenue" -> 10.5, "engagement" -> 0.9, "risk_score" -> 0.1),
        Map("category" -> "finance", "tier" -> "premium")
      ),
      Offer("standard-shopping", 
        Map("revenue" -> 5.2, "engagement" -> 1.3, "risk_score" -> 0.3),
        Map("category" -> "shopping", "tier" -> "standard")
      ),
      Offer("low-value-entertainment", 
        Map("revenue" -> 2.1, "engagement" -> 0.4, "risk_score" -> 0.6),
        Map("category" -> "entertainment", "tier" -> "basic")
      )
    )

    val config = ResolvedRankingConfig(
      rankingStrategy = "expression_based",
      scoringFormula = "0.5 * revenue + 0.3 * engagement - 0.2 * risk_score",
      filters = List.empty,
      maxResults = 10,
      boostFactors = Map("premium" -> 1.2),
      experimentGroups = Map.empty
    )

    val ranker = Ranker.fromConfig(config)
    val result = ranker.rankWithDetails(offers, RankingContext.empty, includeDebugInfo = true)

    result shouldBe a[Right[_, _]]
    val ranking = result.toOption.get

    ranking.rankedOffers should have size 3
    ranking.totalProcessed shouldBe 3

    // Premium finance offer should benefit from both formula and boost
    val topOffer = ranking.rankedOffers.head
    topOffer.offer.id shouldBe "premium-finance"

    // All offers should have positive scores
    ranking.rankedOffers.foreach { scoredOffer =>
      scoredOffer.score should be > 0.0
    }

    // Debug info should be present
    ranking.debugInfo should not be empty
    ranking.debugInfo should contain key "processingTimeMs"
    ranking.debugInfo should contain key "scoringFormula"
  }
}