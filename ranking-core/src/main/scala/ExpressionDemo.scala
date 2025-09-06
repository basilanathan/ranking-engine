import com.example.ranking.core._

object ExpressionDemo extends App {
  println("🚀 Expression-Based Ranking Demo")
  println("=" * 50)
  
  // Sample feature data
  val features = Map(
    "revenue" -> 1.2,
    "engagement" -> 0.8,
    "risk_score" -> 0.3,
    "popularity" -> 2.5
  )
  
  println("📊 Available Features:")
  features.foreach { case (key, value) => println(s"  $key: $value") }
  println()
  
  // Test basic expression evaluation
  val expression = "0.6 * revenue + 0.4 * engagement - 0.1 * risk_score"
  println(s"Evaluating: $expression")
  
  ExpressionEvaluator.evaluate(expression, features) match {
    case Right(result) => println(s"✅ Result: $result")
    case Left(error) => println(s"❌ Error: $error")
  }
  println()
  
  // Test complex expressions with functions
  val complexExpression = "sqrt(revenue * popularity) + max(engagement, 0.5) - abs(risk_score - 0.4)"
  println(s"Complex Expression: $complexExpression")
  
  ExpressionEvaluator.evaluate(complexExpression, features) match {
    case Right(result) => println(s"✅ Result: $result")
    case Left(error) => println(s"❌ Error: $error")
  }
  println()
  
  // Test variable extraction
  // Test with offers
  val offers = List(
    Offer("offer1", Map("revenue" -> 2.5, "engagement" -> 0.8, "risk_score" -> 0.2)),
    Offer("offer2", Map("revenue" -> 1.0, "engagement" -> 1.2, "risk_score" -> 0.5)),
    Offer("offer3", Map("revenue" -> 3.0, "engagement" -> 0.3, "risk_score" -> 0.1))
  )
  
  println("📦 Test Offers:")
  offers.foreach { offer =>
    println(s"  ${offer.id}: revenue=${offer.features("revenue")}, engagement=${offer.features("engagement")}, risk=${offer.features("risk_score")}")
  }
  println()
  
  // Create a ranking configuration
  val config = ResolvedRankingConfig(
    rankingStrategy = "expression_based",
    scoringFormula = "0.7 * revenue + 0.2 * engagement - 0.1 * risk_score",
    filters = List.empty,
    maxResults = 10,
    boostFactors = Map("premium" -> 1.2),
    experimentGroups = Map.empty
  )
  
  val ranker = Ranker.fromConfig(config)
  
  ranker.rank(offers) match {
    case Right(result) =>
      println("🏆 Ranking Results:")
      result.rankedOffers.zipWithIndex.foreach { case (scoredOffer, index) =>
        println(s"  ${index + 1}. ${scoredOffer.offer.id} (score: ${f"${scoredOffer.score}%.2f"})")
      }
      println()
      
      // Show detailed results
      ranker.rankWithDetails(offers, RankingContext.empty, includeDebugInfo = true) match {
        case Right(detailedResult) =>
          println("📈 Detailed Scoring:")
          detailedResult.rankedOffers.foreach { scoredOffer =>
            println(s"  ${scoredOffer.offer.id}:")
            println(s"    Score: ${f"${scoredOffer.score}%.2f"}")
            println(s"    Applied Boosts: ${scoredOffer.appliedBoosts}")
            if (scoredOffer.debugInfo.nonEmpty) {
              println(s"    Debug Info: ${scoredOffer.debugInfo}")
            }
          }
        case Left(error) =>
          println(s"❌ Error in detailed ranking: $error")
      }
      
    case Left(error) =>
      println(s"❌ Ranking failed: $error")
  }
  
  println()
  println("🎉 Demo completed successfully!")
}