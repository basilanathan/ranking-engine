error id: file://<WORKSPACE>/TestConfig.scala:`<error>`#`<error>`.
file://<WORKSPACE>/TestConfig.scala
empty definition using pc, found symbol in pc: 
empty definition using semanticdb

found definition using fallback; symbol fromYamlSchemaFile
offset: 104
uri: file://<WORKSPACE>/TestConfig.scala
text:
```scala
import com.example.ranking.config._

object TestConfig extends App {
  val result = RankingConfig.fromYa@@mlSchemaFile("sample-ranking-config.yml")
  result match {
    case Right(config) =>
      println("✅ Config parsed successfully!")
      println(s"Strategy: ${config.rankingStrategy}")
      println(s"Formula: ${config.scoringFormula}")
      println(s"Filters: ${config.filters.map(_.size).getOrElse(0)}")
      println(s"Experiment groups: ${config.experimentGroups.map(_.keys.mkString(", ")).getOrElse("None")}")
      
      // Test resolving with experiment group
      val resolved = ConfigResolver.resolve(config, Some("high_value_users"))
      resolved match {
        case Right(r) =>
          println(s"✅ Resolved config for high_value_users:")
          println(s"  Formula: ${r.scoringFormula}")
          println(s"  Filters: ${r.filters.size}")
        case Left(errors) =>
          println(s"❌ Resolution errors: ${errors.mkString(", ")}")
      }
      
    case Left(error) =>
      println(s"❌ Failed to parse config: $error")
  }
}
```


#### Short summary: 

empty definition using pc, found symbol in pc: 