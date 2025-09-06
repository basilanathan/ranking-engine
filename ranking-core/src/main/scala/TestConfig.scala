import com.example.ranking.core._

object TestConfig extends App {
  println("🔧 Configuration Test Demo")
  println("=" * 40)
  
  // Create a test configuration
  val config = ResolvedRankingConfig(
    rankingStrategy = "expression_based",
    scoringFormula = "0.6 * revenue + 0.4 * engagement",
    filters = List(
      FilterCondition("revenue > 5.0", Some("High revenue offers only"))
    ),
    maxResults = 10,
    boostFactors = Map("premium" -> 1.2, "trending" -> 1.1),
    experimentGroups = Map(
      "high_value_users" -> ExperimentGroupConfig(
        scoringFormula = Some("0.8 * revenue + 0.2 * engagement"),
        boostFactors = Some(Map("premium" -> 1.5)),
        description = Some("Focus on revenue for high-value users")
      ),
      "engagement_test" -> ExperimentGroupConfig(
        scoringFormula = Some("0.3 * revenue + 0.7 * engagement"),
        maxResults = Some(5),
        description = Some("Test engagement-focused ranking")
      )
    )
  )
  
  println("✅ Base config created successfully!")
  println(s"Strategy: ${config.rankingStrategy}")
  println(s"Formula: ${config.scoringFormula}")
  println(s"Filters: ${config.filters.size}")
  println(s"Boost Factors: ${config.boostFactors.keys.mkString(", ")}")
  println(s"Experiment groups: ${config.experimentGroups.keys.mkString(", ")}")
  println()
  
  // Test experiment resolution
  val contextWithExperiment = RankingContext.withExperiment("high_value_users")
  val contextWithoutExperiment = RankingContext.empty
  
  // Test with experiment
  ExperimentResolver.resolveConfig(config, contextWithExperiment) match {
    case Right((resolvedConfig, experimentOverride)) =>
      println("✅ Resolved config for high_value_users:")
      println(s"  Formula: ${resolvedConfig.scoringFormula}")
      println(s"  Boost Factors: ${resolvedConfig.boostFactors}")
      println(s"  Applied Overrides: ${experimentOverride.map(_.appliedOverrides.mkString(", ")).getOrElse("None")}")
    case Left(error) =>
      println(s"❌ Resolution error: $error")
  }
  println()
  
  // Test without experiment
  ExperimentResolver.resolveConfig(config, contextWithoutExperiment) match {
    case Right((resolvedConfig, experimentOverride)) =>
      println("✅ Resolved config without experiment (control):")
      println(s"  Formula: ${resolvedConfig.scoringFormula}")
      println(s"  Override: ${experimentOverride.map(_.groupName).getOrElse("None")}")
    case Left(error) =>
      println(s"❌ Resolution error: $error")
  }
  println()
  
  println("🎉 Configuration test completed!")
}