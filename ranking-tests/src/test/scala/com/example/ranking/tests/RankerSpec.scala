package com.example.ranking.tests

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import com.example.ranking.core._

class RankerSpec extends AnyFlatSpec with Matchers {

  val sampleOffers = List(
    Offer("offer1", 
      features = Map("revenue" -> 2.5, "engagement" -> 0.8, "risk_score" -> 0.2),
      metadata = Map("category" -> "finance", "tier" -> "premium", "isFraudSuppressed" -> false)
    ),
    Offer("offer2",
      features = Map("revenue" -> 1.0, "engagement" -> 1.2, "risk_score" -> 0.5),
      metadata = Map("category" -> "shopping", "tier" -> "standard", "isFraudSuppressed" -> false)
    ),
    Offer("offer3",
      features = Map("revenue" -> 3.0, "engagement" -> 0.3, "risk_score" -> 0.1),
      metadata = Map("category" -> "entertainment", "tier" -> "premium", "isFraudSuppressed" -> true)
    ),
    Offer("offer4",
      features = Map("revenue" -> 1.8, "engagement" -> 1.5, "risk_score" -> 0.3),
      metadata = Map("category" -> "travel", "tier" -> "standard", "trending" -> true, "isFraudSuppressed" -> false)
    )
  )

  "Ranker" should "rank offers using expression-based scoring" in {
    val config = ResolvedRankingConfig(
      rankingStrategy = "expression_based",
      scoringFormula = "0.6 * revenue + 0.4 * engagement - 0.1 * risk_score",
      filters = List.empty,
      maxResults = 10,
      boostFactors = Map.empty
    )

    val ranker = Ranker.fromConfig(config)
    val result = ranker.rank(sampleOffers)

    result shouldBe a [Right[_, _]]
    val ranking = result.toOption.get
    
    ranking.rankedOffers should have size 4
    ranking.totalProcessed shouldBe 4
    ranking.totalFiltered shouldBe 0

    // Verify scores are calculated correctly and sorted in descending order
    val scores = ranking.rankedOffers.map(_.score)
    scores shouldEqual scores.sorted(Ordering[Double].reverse)
  }

  it should "apply filters correctly" in {
    val filters = List(
      FilterCondition("isFraudSuppressed == false", Some("Exclude fraud suppressed offers")),
      FilterCondition("revenue >= 1.5", Some("Minimum revenue threshold"))
    )

    val config = ResolvedRankingConfig(
      rankingStrategy = "expression_based", 
      scoringFormula = "revenue + engagement",
      filters = filters,
      maxResults = 10,
      boostFactors = Map.empty
    )

    val ranker = Ranker.fromConfig(config)
    val result = ranker.rank(sampleOffers)

    result shouldBe a [Right[_, _]]
    val ranking = result.toOption.get

    // Should filter out offer3 (fraud suppressed) and offer2 (revenue < 1.5)
    ranking.rankedOffers should have size 2
    ranking.totalFiltered shouldBe 2
    ranking.rankedOffers.map(_.offer.id) should contain allOf("offer1", "offer4")
  }

  it should "apply boost factors" in {
    val config = ResolvedRankingConfig(
      rankingStrategy = "expression_based",
      scoringFormula = "revenue",
      filters = List.empty,
      maxResults = 10,
      boostFactors = Map("premium" -> 1.5, "trending" -> 1.2)
    )

    val ranker = Ranker.fromConfig(config)
    val result = ranker.rank(sampleOffers)

    result shouldBe a [Right[_, _]]
    val ranking = result.toOption.get

    // Premium offers (offer1, offer3) should get premium boost
    // Trending offer (offer4) should get trending boost
    val offer1Score = ranking.rankedOffers.find(_.offer.id == "offer1").get.score
    val offer2Score = ranking.rankedOffers.find(_.offer.id == "offer2").get.score

    // offer1 has premium tier so should get boost, offer2 is standard so no boost
    offer1Score should be > (1.5 * 2.5 * 0.9) // revenue * premium boost * margin for floating point
    offer2Score should be < (1.0 * 1.1) // revenue * small margin
  }

  it should "limit results correctly" in {
    val config = ResolvedRankingConfig(
      rankingStrategy = "expression_based",
      scoringFormula = "revenue",
      filters = List.empty,
      maxResults = 2,
      boostFactors = Map.empty
    )

    val ranker = Ranker.fromConfig(config)
    val result = ranker.rank(sampleOffers)

    result shouldBe a [Right[_, _]]
    val ranking = result.toOption.get

    ranking.rankedOffers should have size 2
    ranking.totalProcessed shouldBe 4
  }

  it should "handle scoring errors gracefully" in {
    val config = ResolvedRankingConfig(
      rankingStrategy = "expression_based",
      scoringFormula = "missing_variable + revenue", // missing_variable doesn't exist
      filters = List.empty,
      maxResults = 10,
      boostFactors = Map.empty
    )

    val ranker = Ranker.fromConfig(config)
    val result = ranker.rank(sampleOffers)

    result shouldBe a [Left[_, _]]
    result.swap.toOption.get should include ("missing_variable")
  }

  it should "handle filter errors gracefully" in {
    val filters = List(
      FilterCondition("invalid_syntax +", Some("Invalid filter"))
    )

    val config = ResolvedRankingConfig(
      rankingStrategy = "expression_based",
      scoringFormula = "revenue",
      filters = filters,
      maxResults = 10,
      boostFactors = Map.empty
    )

    val ranker = Ranker.fromConfig(config)
    val result = ranker.rank(sampleOffers)

    result shouldBe a [Left[_, _]]
  }

  it should "provide detailed debug information" in {
    val config = ResolvedRankingConfig(
      rankingStrategy = "expression_based",
      scoringFormula = "0.7 * revenue + 0.3 * engagement",
      filters = List(FilterCondition("revenue > 1.0")),
      maxResults = 5,
      boostFactors = Map("premium" -> 1.2)
    )

    val ranker = Ranker.fromConfig(config)
    val result = ranker.rankWithDetails(sampleOffers, includeDebugInfo = true)

    result shouldBe a [Right[_, _]]
    val ranking = result.toOption.get

    ranking.debugInfo should not be empty
    ranking.debugInfo should contain key "processingTimeMs"
    ranking.debugInfo should contain key "scoringFormula"
    ranking.debugInfo should contain key "filtersApplied"

    ranking.rankedOffers.foreach { scoredOffer =>
      scoredOffer.debugInfo should contain key "baseScore"
      scoredOffer.debugInfo should contain key "boostedScore"
      scoredOffer.debugInfo should contain key "featuresUsed"
    }
  }

  "Ranker.quickRank" should "provide simple ranking interface" in {
    val result = Ranker.quickRank(sampleOffers, "revenue + engagement", maxResults = 3)

    result shouldBe a [Right[_, _]]
    val rankedOffers = result.toOption.get

    rankedOffers should have size 3
    val scores = rankedOffers.map(_.score)
    scores shouldEqual scores.sorted(Ordering[Double].reverse)
  }

  "Ranker.validateConfig" should "validate configuration before use" in {
    val validConfig = ResolvedRankingConfig(
      rankingStrategy = "expression_based",
      scoringFormula = "revenue + engagement",
      filters = List.empty,
      maxResults = 10,
      boostFactors = Map.empty
    )

    val invalidConfig = ResolvedRankingConfig(
      rankingStrategy = "expression_based", 
      scoringFormula = "revenue +", // Invalid syntax
      filters = List.empty,
      maxResults = 10,
      boostFactors = Map.empty
    )

    Ranker.validateConfig(validConfig) shouldBe a [Right[_, _]]
    Ranker.validateConfig(invalidConfig) shouldBe a [Left[_, _]]
  }

  it should "validate with sample features" in {
    val config = ResolvedRankingConfig(
      rankingStrategy = "expression_based",
      scoringFormula = "revenue + unknown_feature", // unknown_feature not in sample
      filters = List.empty,
      maxResults = 10,
      boostFactors = Map.empty
    )

    val sampleFeatures = Map("revenue" -> 1.0, "engagement" -> 0.5)

    val result = Ranker.validateConfig(config, sampleFeatures)
    result shouldBe a [Left[_, _]]
    result.swap.toOption.get should include ("unknown_feature")
  }

  "FilterEvaluator" should "handle complex filter conditions" in {
    val offer = Offer("test",
      features = Map("revenue" -> 5.0, "engagement" -> 1.2),
      metadata = Map("category" -> "finance", "tier" -> "premium", "user_age" -> 25)
    )

    val filters = List(
      FilterCondition("revenue >= 2.0", Some("Min revenue")),
      FilterCondition("category == 'finance'", Some("Finance only")),
      FilterCondition("user_age > 21", Some("Adult users only"))
    )

    filters.foreach { filter =>
      val result = FilterEvaluator.evaluateFilters(offer, List(filter))
      result shouldBe a [Right[_, _]]
      result.toOption.get.passedAll shouldBe true
    }
  }

  "Offer" should "convert from Item correctly" in {
    val item = Item("test", 2.5, Map("revenue" -> "1.5", "category" -> "shopping"))
    val offer = Offer.fromItem(item)

    offer.id shouldBe "test"
    offer.features should contain ("base_score" -> 2.5)
    offer.features should contain ("revenue" -> 1.5)
    offer.metadata should contain ("category" -> "shopping")
  }

  it should "provide convenient helper methods" in {
    val offer = Offer("test", Map("revenue" -> 2.0), Map("category" -> "finance"))

    offer.hasFeature("revenue") shouldBe true
    offer.hasFeature("missing") shouldBe false
    offer.getFeature("revenue") shouldBe Some(2.0)
    offer.getMetadataString("category") shouldBe Some("finance")
  }
}