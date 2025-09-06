package com.example.ranking.tests

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import com.example.ranking.config._

class ConfigSpec extends AnyFlatSpec with Matchers {
  
  "RankingConfig" should "parse valid YAML" in {
    val yaml = """
      |strategy: weighted
      |weights:
      |  popularity: 0.6
      |  recency: 0.4
      |parameters:
      |  threshold: "0.5"
      |""".stripMargin
    
    val result = RankingConfig.fromYaml(yaml)
    result shouldBe a [Right[_, _]]
    
    val config = result.right.get
    config.strategy shouldBe "weighted"
    config.weights shouldBe Map("popularity" -> 0.6, "recency" -> 0.4)
    config.parameters shouldBe Map("threshold" -> "0.5")
  }
  
  "ConfigValidator" should "validate supported strategies" in {
    val validConfig = RankingConfig("score-based")
    val invalidConfig = RankingConfig("unknown-strategy")
    
    ConfigValidator.validate(validConfig) shouldBe a [Right[_, _]]
    ConfigValidator.validate(invalidConfig) shouldBe a [Left[_, _]]
  }
  
  it should "validate weight ranges" in {
    val invalidConfig = RankingConfig("weighted", Map("test" -> 1.5))
    
    val result = ConfigValidator.validate(invalidConfig)
    result shouldBe a [Left[_, _]]
    result.left.get should contain (InvalidWeight("test", 1.5))
  }
  
  it should "require weights for weighted strategy" in {
    val config = RankingConfig("weighted")
    
    val result = ConfigValidator.validate(config)
    result shouldBe a [Left[_, _]]
    result.left.get should contain (MissingRequiredParameter("weights"))
  }
}