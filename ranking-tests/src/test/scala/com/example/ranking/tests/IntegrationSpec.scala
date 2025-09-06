package com.example.ranking.tests

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import com.example.ranking.api._
import com.example.ranking.core._
import com.example.ranking.config._

class IntegrationSpec extends AnyFlatSpec with Matchers {
  
  "RankingService" should "handle requests without config" in {
    val items = List(
      Item("1", 0.8),
      Item("2", 0.5),
      Item("3", 0.9)
    )
    
    val request = RankingRequest(items)
    val result = RankingService.rank(request)
    
    result shouldBe a [Right[_, _]]
    val response = result.right.get
    response.rankedItems.map(_.id) shouldBe List("3", "1", "2")
  }
  
  it should "handle requests with valid config" in {
    val items = List(
      Item("1", 0.5, Map("popularity" -> "0.8")),
      Item("2", 0.7, Map("popularity" -> "0.3"))
    )
    
    val config = RankingConfig("weighted", Map("popularity" -> 0.5))
    val request = RankingRequest(items, Some(config))
    val result = RankingService.rank(request)
    
    result shouldBe a [Right[_, _]]
    val response = result.right.get
    response.rankedItems should have size 2
  }
  
  it should "reject invalid configurations" in {
    val items = List(Item("1", 0.5))
    val invalidConfig = RankingConfig("unknown-strategy")
    val request = RankingRequest(items, Some(invalidConfig))
    
    val result = RankingService.rank(request)
    result shouldBe a [Left[_, _]]
  }
}