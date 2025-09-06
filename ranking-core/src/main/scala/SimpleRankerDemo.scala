import com.example.ranking.core._

object SimpleRankerDemo extends App {
  println("🎯 Simple Ranker Demo")
  println("=" * 40)
  
  // Sample offers
  val offers = List(
    Offer("high-revenue", 
      features = Map("revenue" -> 15.0, "engagement" -> 1.2),
      metadata = Map("category" -> "finance", "tier" -> "premium")
    ),
    Offer("high-engagement",
      features = Map("revenue" -> 8.5, "engagement" -> 2.8),
      metadata = Map("category" -> "shopping", "trending" -> true)
    ),
    Offer("balanced-offer",
      features = Map("revenue" -> 12.0, "engagement" -> 1.8),
      metadata = Map("category" -> "travel", "tier" -> "premium")
    ),
    Offer("low-value",
      features = Map("revenue" -> 3.2, "engagement" -> 0.4),
      metadata = Map("category" -> "misc")
    )
  )
  
  println("📊 Input Offers:")
  offers.foreach { offer =>
    println(s"  ${offer.id}: revenue=${offer.features("revenue")}, engagement=${offer.features("engagement")}")
  }
  println()
  
  // Simple ranking without filters
  val config = ResolvedRankingConfig(
    rankingStrategy = "expression_based",
    scoringFormula = "0.6 * revenue + 0.4 * engagement",
    filters = List.empty, // No filters to avoid parsing issues
    maxResults = 10,
    boostFactors = Map("premium" -> 1.2, "trending" -> 1.1)
  )
  
  val ranker = Ranker.fromConfig(config)
  val result = ranker.rank(offers)
  
  result match {
    case Right(ranking) =>
      println("🏆 Ranking Results:")
      ranking.rankedOffers.zipWithIndex.foreach { case (scoredOffer, index) =>
        val offer = scoredOffer.offer
        println(s"${index + 1}. ${offer.id}")
        println(s"   Score: ${scoredOffer.score.formatted("%.2f")}")
        println(s"   Revenue: ${offer.features("revenue")}, Engagement: ${offer.features("engagement")}")
        println(s"   Category: ${offer.metadata.get("category").getOrElse("N/A")}")
        println()
      }
      
      println(s"✅ Successfully ranked ${ranking.rankedOffers.size} offers!")
      
    case Left(error) =>
      println(s"❌ Ranking failed: $error")
  }
  
  println()
  println("🚀 Testing Expression Evaluator directly:")
  val features = Map("revenue" -> 10.0, "engagement" -> 1.5, "risk_score" -> 0.3)
  
  List(
    "revenue + engagement",
    "0.7 * revenue + 0.3 * engagement",
    "revenue * engagement - risk_score",
    "sqrt(revenue) + max(engagement, 1.0)"
  ).foreach { formula =>
    ExpressionEvaluator.evaluate(formula, features) match {
      case Right(result) => println(s"  $formula = ${result.formatted("%.3f")}")
      case Left(error) => println(s"  $formula = ERROR: $error")
    }
  }
  
  println()
  println("✅ Demo completed!")
}