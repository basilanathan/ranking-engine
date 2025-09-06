package com.example.ranking.tests

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import com.example.ranking.core._

class ExperimentRankerSpec extends AnyFlatSpec with Matchers {

  val sampleOffers = List(
    Offer("high-revenue", 
      features = Map("revenue" -> 15.0, "engagement" -> 1.2, "conversion_rate" -> 0.08),
      metadata = Map("category" -> "finance", "tier" -> "premium")
    ),
    Offer("high-engagement",
      features = Map("revenue" -> 8.5, "engagement" -> 2.8, "conversion_rate" -> 0.15),
      metadata = Map("category" -> "shopping", "trending" -> true)
    ),
    Offer("balanced-offer",
      features = Map("revenue" -> 12.0, "engagement" -> 1.8, "conversion_rate" -> 0.10),
      metadata = Map("category" -> "travel", "tier" -> "premium")
    ),
    Offer("conversion-champion",
      features = Map("revenue" -> 9.0, "engagement" -> 1.0, "conversion_rate" -> 0.25),
      metadata = Map("category" -> "gaming")
    )
  )

  "Ranker with experiment overrides" should "use base formula when no experiment group is specified" in {
    val baseConfig = ResolvedRankingConfig(
      rankingStrategy = "expression_based",
      scoringFormula = "0.7 * revenue + 0.3 * engagement",
      filters = List.empty,
      maxResults = 10,
      boostFactors = Map.empty,
      experimentGroups = Map(
        "conversion_focus" -> ExperimentGroupConfig(
          scoringFormula = Some("0.4 * revenue + 0.2 * engagement + 0.4 * conversion_rate * 100")
        )
      )
    )

    val ranker = Ranker.fromConfig(baseConfig)
    val context = RankingContext.empty // No experiment group
    val result = ranker.rank(sampleOffers, context)

    result shouldBe a [Right[_, _]]
    val ranking = result.toOption.get

    // Should use base formula: 0.7 * revenue + 0.3 * engagement
    ranking.experimentOverride shouldBe None
    
    // High revenue offer should rank first with base formula
    ranking.rankedOffers.head.offer.id shouldBe "high-revenue"
  }

  it should "apply experiment formula when experiment group matches" in {
    val baseConfig = ResolvedRankingConfig(
      rankingStrategy = "expression_based",
      scoringFormula = "0.7 * revenue + 0.3 * engagement",
      filters = List.empty,
      maxResults = 10,
      boostFactors = Map.empty,
      experimentGroups = Map(
        "conversion_focus" -> ExperimentGroupConfig(
          scoringFormula = Some("0.4 * revenue + 0.2 * engagement + 0.4 * conversion_rate * 100"),
          description = Some("Focus on conversion rate optimization")
        )
      )
    )

    val ranker = Ranker.fromConfig(baseConfig)
    val context = RankingContext.withExperiment("conversion_focus")
    val result = ranker.rankWithDetails(sampleOffers, context, includeDebugInfo = true)

    result shouldBe a [Right[_, _]]
    val ranking = result.toOption.get

    // Should use experiment formula
    ranking.experimentOverride shouldBe defined
    ranking.experimentOverride.get.groupName shouldBe "conversion_focus"
    ranking.experimentOverride.get.appliedOverrides should contain ("scoringFormula")
    ranking.debugInfo.get("scoringFormula") shouldBe Some("0.4 * revenue + 0.2 * engagement + 0.4 * conversion_rate * 100")

    // With conversion-focused formula, conversion_champion should rank higher
    val conversionOfferRank = ranking.rankedOffers.indexWhere(_.offer.id == "conversion-champion")
    val revenueOfferRank = ranking.rankedOffers.indexWhere(_.offer.id == "high-revenue")
    
    // Conversion champion should rank better than it would with base formula
    conversionOfferRank should be < 3 // Should be in top 3
  }

  it should "apply multiple experiment overrides" in {
    val baseConfig = ResolvedRankingConfig(
      rankingStrategy = "expression_based",
      scoringFormula = "revenue + engagement",
      filters = List.empty,
      maxResults = 10,
      boostFactors = Map("premium" -> 1.2),
      experimentGroups = Map(
        "multi_override" -> ExperimentGroupConfig(
          scoringFormula = Some("0.5 * revenue + 0.5 * engagement"),
          boostFactors = Some(Map("premium" -> 1.5, "trending" -> 1.3)),
          maxResults = Some(3)
        )
      )
    )

    val ranker = Ranker.fromConfig(baseConfig)
    val context = RankingContext.withExperiment("multi_override")
    val result = ranker.rankWithDetails(sampleOffers, context)

    result shouldBe a [Right[_, _]]
    val ranking = result.toOption.get

    // Should apply all overrides
    ranking.experimentOverride shouldBe defined
    val experimentOverride = ranking.experimentOverride.get
    experimentOverride.appliedOverrides should contain allOf ("scoringFormula", "boostFactors", "maxResults")

    // Should be limited by max results override
    ranking.rankedOffers.size should be <= 3 // Max results override
  }

  it should "handle non-existent experiment groups gracefully" in {
    val baseConfig = ResolvedRankingConfig(
      rankingStrategy = "expression_based",
      scoringFormula = "revenue + engagement",
      filters = List.empty,
      maxResults = 10,
      boostFactors = Map.empty,
      experimentGroups = Map(
        "valid_experiment" -> ExperimentGroupConfig(scoringFormula = Some("revenue * 2"))
      )
    )

    val ranker = Ranker.fromConfig(baseConfig)
    val context = RankingContext.withExperiment("non_existent_experiment")
    val result = ranker.rank(sampleOffers, context)

    result shouldBe a [Right[_, _]]
    val ranking = result.toOption.get

    // Should use base config when experiment group doesn't exist
    ranking.experimentOverride shouldBe None
  }

  it should "validate experiment formulas and reject invalid ones" in {
    val baseConfig = ResolvedRankingConfig(
      rankingStrategy = "expression_based",
      scoringFormula = "revenue + engagement",
      filters = List.empty,
      maxResults = 10,
      boostFactors = Map.empty,
      experimentGroups = Map(
        "invalid_formula" -> ExperimentGroupConfig(
          scoringFormula = Some("revenue +") // Invalid formula
        )
      )
    )

    val ranker = Ranker.fromConfig(baseConfig)
    val context = RankingContext.withExperiment("invalid_formula")
    val result = ranker.rank(sampleOffers, context)

    result shouldBe a [Left[_, _]]
    result.swap.toOption.get should include ("Invalid experiment formula")
  }

  "RankingContext" should "provide convenient builders" in {
    val context1 = RankingContext.withExperiment("test_group")
    context1.experimentGroup shouldBe Some("test_group")
    context1.hasExperiment shouldBe true
    context1.isInExperiment("test_group") shouldBe true
    context1.isInExperiment("other_group") shouldBe false

    val context2 = RankingContext.empty
      .withExperiment("exp1")
      .withUser("user123")
      .withSession("session456")
      .withMetadata("source", "mobile_app")

    context2.experimentGroup shouldBe Some("exp1")
    context2.userId shouldBe Some("user123")
    context2.sessionId shouldBe Some("session456")
    context2.metadata should contain ("source" -> "mobile_app")
  }

  "ExperimentResolver" should "resolve configurations correctly" in {
    val baseConfig = ResolvedRankingConfig(
      rankingStrategy = "base",
      scoringFormula = "base_formula",
      filters = List(FilterCondition("base_filter")),
      maxResults = 100,
      boostFactors = Map("base_boost" -> 1.0),
      experimentGroups = Map(
        "test_exp" -> ExperimentGroupConfig(
          scoringFormula = Some("experiment_formula"),
          boostFactors = Some(Map("exp_boost" -> 2.0))
        )
      )
    )

    // Test with no experiment
    val (resolved1, override1) = ExperimentResolver.resolveConfig(baseConfig, RankingContext.empty).toOption.get
    resolved1.scoringFormula shouldBe "base_formula"
    override1 shouldBe None

    // Test with experiment
    val context = RankingContext.withExperiment("test_exp")
    val (resolved2, override2) = ExperimentResolver.resolveConfig(baseConfig, context).toOption.get
    
    resolved2.scoringFormula shouldBe "experiment_formula"
    resolved2.boostFactors should contain ("exp_boost" -> 2.0)
    resolved2.boostFactors should contain ("base_boost" -> 1.0) // Should merge, not replace
    
    override2 shouldBe defined
    override2.get.appliedOverrides should contain allOf ("scoringFormula", "boostFactors")
  }

  "Ranker.quickRankWithExperiment" should "provide simple experiment API" in {
    val context = RankingContext.withExperiment("test")
    val result = Ranker.quickRankWithExperiment(
      offers = sampleOffers,
      formula = "revenue * 2", 
      context = context,
      maxResults = 3
    )

    result shouldBe a [Right[_, _]]
    val rankedOffers = result.toOption.get
    rankedOffers should have size 3
    
    // Should be sorted by revenue * 2 (descending)
    val scores = rankedOffers.map(_.score)
    scores shouldEqual scores.sorted(Ordering[Double].reverse)
  }
}