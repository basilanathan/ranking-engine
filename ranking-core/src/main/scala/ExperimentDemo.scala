import com.example.ranking.core._

object ExperimentDemo extends App {
  println("🧪 Experiment Override Demo")
  println("=" * 50)
  
  // Sample offers with different characteristics
  val offers = List(
    Offer("high-revenue-offer", 
      features = Map("revenue" -> 20.0, "engagement" -> 1.0, "conversion_rate" -> 0.05),
      metadata = Map("category" -> "finance", "tier" -> "premium")
    ),
    Offer("high-engagement-offer",
      features = Map("revenue" -> 8.0, "engagement" -> 3.5, "conversion_rate" -> 0.12),
      metadata = Map("category" -> "social", "trending" -> true)
    ),
    Offer("high-conversion-offer",
      features = Map("revenue" -> 10.0, "engagement" -> 1.5, "conversion_rate" -> 0.30),
      metadata = Map("category" -> "gaming", "personalized" -> true)
    ),
    Offer("balanced-offer",
      features = Map("revenue" -> 12.0, "engagement" -> 2.0, "conversion_rate" -> 0.15),
      metadata = Map("category" -> "shopping")
    )
  )
  
  println("📊 Input Offers:")
  offers.foreach { offer =>
    val features = offer.features
    println(s"  ${offer.id}:")
    println(s"    Revenue: ${features("revenue")}, Engagement: ${features("engagement")}, Conversion: ${features("conversion_rate")}")
  }
  println()
  
  // Create configuration with experiment groups
  val config = ResolvedRankingConfig(
    rankingStrategy = "expression_based",
    scoringFormula = "0.6 * revenue + 0.4 * engagement", // Base formula favors revenue
    filters = List.empty,
    maxResults = 5,
    boostFactors = Map("premium" -> 1.1, "trending" -> 1.05),
    experimentGroups = Map(
      "engagement_focus" -> ExperimentGroupConfig(
        scoringFormula = Some("0.2 * revenue + 0.8 * engagement"), // Favor engagement
        description = Some("Experiment focusing on engagement over revenue")
      ),
      "conversion_optimization" -> ExperimentGroupConfig(
        scoringFormula = Some("0.3 * revenue + 0.2 * engagement + 0.5 * conversion_rate * 100"), // Focus on conversion
        boostFactors = Some(Map("personalized" -> 1.25)), // Add personalization boost
        description = Some("Optimize for conversion rate")
      ),
      "premium_boost" -> ExperimentGroupConfig(
        boostFactors = Some(Map("premium" -> 1.5, "trending" -> 1.2)), // Increase boosts
        maxResults = Some(3), // Limit results
        description = Some("Higher boosts for premium content")
      )
    )
  )
  
  val ranker = Ranker.fromConfig(config)
  
  // Test different experiment scenarios
  val scenarios = List(
    ("Control Group (Base Formula)", RankingContext.empty),
    ("Engagement Focus Experiment", RankingContext.withExperiment("engagement_focus")),
    ("Conversion Optimization", RankingContext.withExperiment("conversion_optimization")),
    ("Premium Boost Test", RankingContext.withExperiment("premium_boost")),
    ("Non-existent Experiment", RankingContext.withExperiment("non_existent"))
  )
  
  scenarios.foreach { case (scenarioName, context) =>
    println(s"🔬 $scenarioName")
    println("-" * 40)
    
    ranker.rankWithDetails(offers, context, includeDebugInfo = true) match {
      case Right(result) =>
        println(s"Formula Used: ${result.debugInfo.get("scoringFormula").getOrElse("N/A")}")
        println(s"Experiment Group: ${result.debugInfo.get("experimentGroup").getOrElse("None")}")
        
        result.experimentOverride match {
          case Some(experimentOverride) =>
            println(s"Experiment: ${experimentOverride.groupName}")
            println(s"Overrides Applied: ${experimentOverride.appliedOverrides.mkString(", ")}")
          case None =>
            println("Experiment: None (using base configuration)")
        }
        
        println("Rankings:")
        result.rankedOffers.zipWithIndex.foreach { case (scoredOffer, index) =>
          val offer = scoredOffer.offer
          println(s"  ${index + 1}. ${offer.id}")
          println(s"     Score: ${scoredOffer.score.formatted("%.2f")}")
          println(s"     Base Score: ${scoredOffer.debugInfo.get("baseScore").map(_.toString.take(5)).getOrElse("N/A")}")
        }
        
      case Left(error) =>
        println(s"❌ Error: $error")
    }
    println()
  }
  
  // Demonstrate A/B testing scenario
  println("🎯 A/B Testing Simulation")
  println("-" * 30)
  
  val users = List("user1", "user2", "user3", "user4", "user5")
  val experiments = List("control", "engagement_focus", "conversion_optimization")
  
  users.foreach { userId =>
    // Simulate assignment to experiment groups (simple hash-based)
    val experimentIndex = Math.abs(userId.hashCode) % experiments.size
    val experimentGroup = experiments(experimentIndex)
    
    val context = if (experimentGroup == "control") {
      RankingContext.withUser(userId)
    } else {
      RankingContext.withUser(userId).withExperiment(experimentGroup)
    }
    
    ranker.rank(offers, context) match {
      case Right(result) =>
        val topOffer = result.rankedOffers.head
        val experimentInfo = result.experimentOverride.map(_.groupName).getOrElse("control")
        println(s"$userId -> Experiment: $experimentInfo, Top Offer: ${topOffer.offer.id} (${topOffer.score.formatted("%.2f")})")
        
      case Left(error) =>
        println(s"$userId -> Error: $error")
    }
  }
  
  println()
  println("✅ Experiment Demo Completed!")
  println()
  println("🔍 Key Observations:")
  println("- Control group uses base revenue-focused formula")
  println("- Engagement experiment promotes high-engagement offers")  
  println("- Conversion experiment boosts high-conversion-rate offers")
  println("- Premium boost experiment limits results and increases boosts")
  println("- Non-existent experiments gracefully fall back to base config")
}