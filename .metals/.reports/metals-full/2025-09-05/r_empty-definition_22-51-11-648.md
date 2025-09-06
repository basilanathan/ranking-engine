error id: file://<WORKSPACE>/ExpressionDemo.scala:`<error>`#`<error>`.
file://<WORKSPACE>/ExpressionDemo.scala
empty definition using pc, found symbol in pc: 
empty definition using semanticdb

found definition using fallback; symbol evaluate
offset: 1023
uri: file://<WORKSPACE>/ExpressionDemo.scala
text:
```scala
import com.example.ranking.core._
import com.example.ranking.config._

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
  println(s"🧮 Evaluating: $expression")
  
  ExpressionEvaluator.evaluate(expression, features) match {
    case Right(result) => println(s"✅ Result: $result")
    case Left(error) => println(s"❌ Error: $error")
  }
  println()
  
  // Test complex expressions with functions
  val complexExpression = "sqrt(revenue * popularity) + max(engagement, 0.5) - abs(risk_score - 0.4)"
  println(s"🔬 Complex Expression: $complexExpression")
  
  ExpressionEvaluator.eval@@uate(complexExpression, features) match {
    case Right(result) => println(s"✅ Result: $result")
    case Left(error) => println(s"❌ Error: $error")
  }
  println()
  
  // Test variable extraction
  ExpressionEvaluator.extractVariables(expression) match {
    case Right(vars) => println(s"📝 Variables found: ${vars.mkString(", ")}")
    case Left(error) => println(s"❌ Error extracting variables: $error")
  }
  println()
  
  // Test with ranking items
  val items = List(
    Item("offer1", 1.0, Map("revenue" -> "2.5", "engagement" -> "0.8", "risk_score" -> "0.2")),
    Item("offer2", 1.0, Map("revenue" -> "1.0", "engagement" -> "1.2", "risk_score" -> "0.5")),
    Item("offer3", 1.0, Map("revenue" -> "3.0", "engagement" -> "0.3", "risk_score" -> "0.1"))
  )
  
  println("🎯 Ranking Items with Expression-Based Strategy")
  println("Items:")
  items.foreach { item =>
    println(s"  ${item.id}: revenue=${item.metadata("revenue")}, engagement=${item.metadata("engagement")}, risk=${item.metadata("risk_score")}")
  }
  println()
  
  val strategy = ExpressionBasedRankingStrategy.fromConfig(
    formula = "0.7 * revenue + 0.2 * engagement - 0.1 * risk_score",
    boosts = Map("premium" -> 1.2)
  ).toOption.get
  
  val rankedItems = strategy.rank(items)
  println("🏆 Ranking Results:")
  rankedItems.zipWithIndex.foreach { case (item, index) =>
    println(s"  ${index + 1}. ${item.id} (score: ${item.score})")
  }
  println()
  
  // Show detailed scoring
  val detailedResults = strategy.rankWithDetails(items)
  println("📈 Detailed Scoring:")
  detailedResults.foreach {
    case Right(scored) =>
      println(s"  ${scored.item.id}:")
      println(s"    Formula Score: ${scored.formulaScore}")
      println(s"    Final Score: ${scored.score}")
      println(s"    Applied Boosts: ${scored.appliedBoosts}")
    case Left(error) =>
      println(s"  Error: $error")
  }
  println()
  
  println("🎉 Demo completed successfully!")
}
```


#### Short summary: 

empty definition using pc, found symbol in pc: 