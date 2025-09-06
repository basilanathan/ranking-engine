error id: file://<WORKSPACE>/RankerDemo.scala:`<error>`#`<error>`.
file://<WORKSPACE>/RankerDemo.scala
empty definition using pc, found symbol in pc: 
empty definition using semanticdb

found definition using fallback; symbol FilterCondition
offset: 2317
uri: file://<WORKSPACE>/RankerDemo.scala
text:
```scala
import com.example.ranking.core._

object RankerDemo extends App {
  println("🎯 Ranker Demo - Complete Offer Ranking System")
  println("=" * 60)
  
  // Sample offers with rich feature data
  val offers = List(
    Offer("premium-finance-1", 
      features = Map("revenue" -> 15.0, "engagement" -> 1.2, "risk_score" -> 0.1, "conversion_rate" -> 0.08),
      metadata = Map("category" -> "finance", "tier" -> "premium", "isFraudSuppressed" -> false, "user_segment" -> "high_value")
    ),
    Offer("standard-shopping-1",
      features = Map("revenue" -> 8.5, "engagement" -> 1.8, "risk_score" -> 0.3, "conversion_rate" -> 0.12),
      metadata = Map("category" -> "shopping", "tier" -> "standard", "isFraudSuppressed" -> false, "trending" -> true)
    ),
    Offer("premium-travel-1",
      features = Map("revenue" -> 22.0, "engagement" -> 0.9, "risk_score" -> 0.2, "conversion_rate" -> 0.06),
      metadata = Map("category" -> "travel", "tier" -> "premium", "isFraudSuppressed" -> false, "seasonal" -> true)
    ),
    Offer("fraud-entertainment-1",
      features = Map("revenue" -> 12.0, "engagement" -> 2.1, "risk_score" -> 0.8, "conversion_rate" -> 0.15),
      metadata = Map("category" -> "entertainment", "tier" -> "standard", "isFraudSuppressed" -> true)
    ),
    Offer("low-value-offer",
      features = Map("revenue" -> 3.2, "engagement" -> 0.4, "risk_score" -> 0.6, "conversion_rate" -> 0.03),
      metadata = Map("category" -> "misc", "tier" -> "basic", "isFraudSuppressed" -> false)
    ),
    Offer("trending-gaming-1",
      features = Map("revenue" -> 9.8, "engagement" -> 3.2, "risk_score" -> 0.25, "conversion_rate" -> 0.18),
      metadata = Map("category" -> "gaming", "tier" -> "standard", "isFraudSuppressed" -> false, "trending" -> true, "personalized" -> true)
    )
  )
  
  println("📊 Input Offers:")
  offers.foreach { offer =>
    println(s"  ${offer.id}:")
    println(s"    Revenue: ${offer.features("revenue")}, Engagement: ${offer.features("engagement")}")
    println(s"    Category: ${offer.metadata("category")}, Tier: ${offer.metadata("tier")}")
  }
  println()
  
  // Create a sophisticated ranking configuration
  val scoringFormula = "0.4 * revenue + 0.3 * engagement + 0.2 * conversion_rate * 100 - 0.1 * risk_score * 10"
  
  val filters = List(
    FilterConditi@@on("isFraudSuppressed == false", Some("Exclude fraud suppressed offers")),
    FilterCondition("revenue >= 5.0", Some("Minimum revenue threshold")),
    FilterCondition("risk_score <= 0.6", Some("Maximum risk threshold"))
  )
  
  val boostFactors = Map(
    "premium" -> 1.25,    // Premium tier boost
    "trending" -> 1.15,   // Trending boost
    "personalized" -> 1.1 // Personalization boost
  )
  
  val config = ResolvedRankingConfig(
    rankingStrategy = "expression_based",
    scoringFormula = scoringFormula,
    filters = filters,
    maxResults = 5,
    boostFactors = boostFactors
  )
  
  println("⚙️ Ranking Configuration:")
  println(s"  Formula: $scoringFormula")
  println(s"  Filters: ${filters.size} applied")
  println(s"  Boost Factors: ${boostFactors.keys.mkString(", ")}")
  println(s"  Max Results: ${config.maxResults}")
  println()
  
  // Create ranker and rank offers
  val ranker = Ranker.fromConfig(config)
  val result = ranker.rankWithDetails(offers, includeDebugInfo = true)
  
  result match {
    case Right(ranking) =>
      println("🏆 Ranking Results:")
      println(s"  Total processed: ${ranking.totalProcessed}")
      println(s"  Filtered out: ${ranking.totalFiltered}")
      println(s"  Results returned: ${ranking.rankedOffers.size}")
      println()
      
      ranking.rankedOffers.zipWithIndex.foreach { case (scoredOffer, index) =>
        val offer = scoredOffer.offer
        println(s"${index + 1}. ${offer.id} (Score: ${scoredOffer.score.formatted("%.2f")})")
        println(s"   Category: ${offer.metadata("category")}, Tier: ${offer.metadata("tier")}")
        println(s"   Base Score: ${scoredOffer.debugInfo.get("baseScore").map(_.toString.take(5)).getOrElse("N/A")}")
        println(s"   Boost Multiplier: ${scoredOffer.debugInfo.get("boostMultiplier").map(_.toString.take(4)).getOrElse("N/A")}")
        println(s"   Features: Revenue=${offer.features("revenue")}, Engagement=${offer.features("engagement")}")
        println()
      }
      
      println("📈 Debug Information:")
      ranking.debugInfo.foreach { case (key, value) =>
        println(s"  $key: $value")
      }
      
    case Left(error) =>
      println(s"❌ Ranking failed: $error")
  }
  
  println()
  println("🧪 Testing Quick Rank API:")
  val quickResult = Ranker.quickRank(offers, "revenue + engagement * 2", maxResults = 3)
  quickResult match {
    case Right(rankedOffers) =>
      println("Quick ranking results:")
      rankedOffers.zipWithIndex.foreach { case (scoredOffer, index) =>
        println(s"  ${index + 1}. ${scoredOffer.offer.id} - Score: ${scoredOffer.score.formatted("%.2f")}")
      }
    case Left(error) =>
      println(s"Quick ranking failed: $error")
  }
  
  println()
  println("✅ Demo completed successfully!")
}
```


#### Short summary: 

empty definition using pc, found symbol in pc: 