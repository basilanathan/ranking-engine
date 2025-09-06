package com.example.ranking.tests

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import com.example.ranking.config._

class ConfigSchemaSpec extends AnyFlatSpec with Matchers {
  
  "RankingConfigSchema" should "parse the sample YAML config" in {
    val yaml = """
      |ranking_strategy: "revenue_optimized"
      |scoring_formula: "0.6 * engagement + 0.4 * revenue - 0.1 * risk_score"
      |filters:
      |  - condition: "offer.isFraudSuppressed == false"
      |    description: "Exclude fraud-suppressed offers"
      |  - condition: "user.tier >= 2"
      |    description: "Only premium users"
      |experiment_groups:
      |  high_value_users:
      |    scoring_formula: "0.8 * revenue + 0.2 * engagement"
      |    filters:
      |      - condition: "user.ltv > 1000"
      |        description: "High lifetime value users only"
      |max_results: 50
      |boost_factors:
      |  new_offers: 1.2
      |  trending_offers: 1.1
      |""".stripMargin
    
    val result = RankingConfig.fromYamlSchema(yaml)
    result shouldBe a [Right[_, _]]
    
    val config = result.right.get
    config.rankingStrategy shouldBe "revenue_optimized"
    config.scoringFormula shouldBe "0.6 * engagement + 0.4 * revenue - 0.1 * risk_score"
    config.filters.get should have size 2
    config.experimentGroups.get should contain key "high_value_users"
    config.maxResults shouldBe Some(50)
    config.boostFactors.get should contain ("new_offers" -> 1.2)
  }
  
  "ConfigResolver" should "resolve base config correctly" in {
    val config = RankingConfigSchema(
      rankingStrategy = "revenue_optimized",
      scoringFormula = "0.6 * engagement + 0.4 * revenue",
      filters = Some(List(FilterCondition("user.active == true"))),
      maxResults = Some(100),
      boostFactors = Some(Map("new" -> 1.5))
    )
    
    val result = ConfigResolver.resolve(config)
    result shouldBe a [Right[_, _]]
    
    val resolved = result.right.get
    resolved.rankingStrategy shouldBe "revenue_optimized"
    resolved.scoringFormula shouldBe "0.6 * engagement + 0.4 * revenue"
    resolved.filters should have size 1
    resolved.maxResults shouldBe 100
    resolved.boostFactors should contain ("new" -> 1.5)
  }
  
  it should "resolve experiment group overrides" in {
    val experimentGroup = ExperimentGroupConfig(
      scoringFormula = Some("0.9 * engagement + 0.1 * revenue"),
      filters = Some(List(FilterCondition("device.type == 'mobile'")))
    )
    
    val config = RankingConfigSchema(
      rankingStrategy = "base_strategy",
      scoringFormula = "0.5 * engagement + 0.5 * revenue",
      experimentGroups = Some(Map("mobile_group" -> experimentGroup))
    )
    
    val result = ConfigResolver.resolve(config, Some("mobile_group"))
    result shouldBe a [Right[_, _]]
    
    val resolved = result.right.get
    resolved.scoringFormula shouldBe "0.9 * engagement + 0.1 * revenue"
    resolved.filters should have size 1
    resolved.filters.head.condition shouldBe "device.type == 'mobile'"
  }
  
  "ConfigResolver validation" should "reject invalid scoring formulas" in {
    val config = RankingConfigSchema(
      rankingStrategy = "test",
      scoringFormula = ""  // Empty formula
    )
    
    val result = ConfigResolver.resolve(config)
    result shouldBe a [Left[_, _]]
    
    val errors = result.left.get
    errors should contain (InvalidScoringFormula("", "Formula cannot be empty"))
  }
  
  it should "reject invalid filter conditions" in {
    val config = RankingConfigSchema(
      rankingStrategy = "test",
      scoringFormula = "base_score",
      filters = Some(List(FilterCondition("invalid condition")))  // No comparison operator
    )
    
    val result = ConfigResolver.resolve(config)
    result shouldBe a [Left[_, _]]
    
    val errors = result.left.get
    errors should contain (InvalidFilterCondition("invalid condition", "Condition must contain a comparison operator"))
  }
  
  it should "reject invalid boost factors" in {
    val config = RankingConfigSchema(
      rankingStrategy = "test",
      scoringFormula = "base_score",
      boostFactors = Some(Map("negative" -> -1.0, "too_high" -> 15.0))
    )
    
    val result = ConfigResolver.resolve(config)
    result shouldBe a [Left[_, _]]
    
    val errors = result.left.get
    errors should contain (InvalidBoostFactor("negative", -1.0, "Boost factor must be positive"))
    errors should contain (InvalidBoostFactor("too_high", 15.0, "Boost factor too high (max 10.0)"))
  }
}