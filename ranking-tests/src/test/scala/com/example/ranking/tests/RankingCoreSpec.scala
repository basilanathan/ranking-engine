package com.example.ranking.tests

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import com.example.ranking.core._

class RankingCoreSpec extends AnyFlatSpec with Matchers {
  
  "RankingEngine with score-based strategy" should "rank items by score descending" in {
    val items = List(
      Item("1", 0.8),
      Item("2", 0.5),
      Item("3", 0.9)
    )
    
    val engine = RankingEngine.withDefaultStrategy
    val ranked = engine.rank(items)
    
    ranked.map(_.id) shouldBe List("3", "1", "2")
  }
  
  "Weighted ranking strategy" should "apply weights correctly" in {
    val items = List(
      Item("1", 0.5, Map("popularity" -> "0.8", "recency" -> "0.2")),
      Item("2", 0.7, Map("popularity" -> "0.3", "recency" -> "0.9"))
    )
    
    val weights = Map("popularity" -> 0.6, "recency" -> 0.4)
    val strategy = RankingStrategies.weightedRanking(weights)
    val engine = RankingEngine(strategy)
    
    val ranked = engine.rank(items)
    
    // Item 1: 0.5 + (0.8 * 0.6) + (0.2 * 0.4) = 0.5 + 0.48 + 0.08 = 1.06
    // Item 2: 0.7 + (0.3 * 0.6) + (0.9 * 0.4) = 0.7 + 0.18 + 0.36 = 1.24
    ranked.head.id shouldBe "2"
  }
}