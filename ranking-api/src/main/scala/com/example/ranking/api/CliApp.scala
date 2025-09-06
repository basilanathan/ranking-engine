package com.example.ranking.api

import com.example.ranking.core._
import com.example.ranking.config._
import scopt.OParser
import io.circe.parser._
import RankingService._

case class CliConfig(
  configFile: Option[String] = None,
  items: String = "",
  output: Option[String] = None
)

object CliApp {
  
  val builder = OParser.builder[CliConfig]
  val parser = {
    import builder._
    OParser.sequence(
      programName("ranking-engine"),
      head("ranking-engine", "0.1.0"),
      opt[String]('c', "config")
        .action((x, c) => c.copy(configFile = Some(x)))
        .text("path to YAML configuration file"),
      opt[String]('i', "items")
        .required()
        .action((x, c) => c.copy(items = x))
        .text("JSON string of items to rank"),
      opt[String]('o', "output")
        .action((x, c) => c.copy(output = Some(x)))
        .text("output file path (optional)")
    )
  }
  
  def main(args: Array[String]): Unit = {
    OParser.parse(parser, args, CliConfig()) match {
      case Some(config) => run(config)
      case _ => System.exit(1)
    }
  }
  
  private def run(config: CliConfig): Unit = {
    val result = for {
      items <- parseItems(config.items)
      rankingConfig <- config.configFile match {
        case Some(file) => RankingConfig.fromYamlFile(file).map(Some(_))
        case None => Right(None)
      }
      request = RankingRequest(items, rankingConfig)
      response <- RankingService.rank(request).left.map(new RuntimeException(_))
    } yield response
    
    result match {
      case Right(response) =>
        val output = responseEncoder(response).spaces2
        config.output match {
          case Some(file) =>
            val writer = new java.io.PrintWriter(file)
            try writer.write(output)
            finally writer.close()
            println(s"Results written to $file")
          case None => println(output)
        }
      case Left(error) =>
        System.err.println(s"Error: ${error.getMessage}")
        System.exit(1)
    }
  }
  
  private def parseItems(itemsJson: String): Either[Throwable, List[Item]] = {
    parse(itemsJson).flatMap(_.as[List[Item]]).left.map(new RuntimeException(_))
  }
}