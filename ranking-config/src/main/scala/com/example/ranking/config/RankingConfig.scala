package com.example.ranking.config

import io.circe._
import io.circe.generic.semiauto._
import io.circe.yaml.parser

@deprecated("Use RankingConfigSchema instead", "0.2.0")
case class RankingConfig(
  strategy: String,
  weights: Map[String, Double] = Map.empty,
  parameters: Map[String, String] = Map.empty
)

object RankingConfig {
  implicit val decoder: Decoder[RankingConfig] = deriveDecoder[RankingConfig]
  implicit val encoder: Encoder[RankingConfig] = deriveEncoder[RankingConfig]
  
  def fromYaml(yamlString: String): Either[Error, RankingConfig] = {
    parser.parse(yamlString).flatMap(_.as[RankingConfig])
  }
  
  def fromYamlFile(path: String): Either[Throwable, RankingConfig] = {
    scala.util.Try {
      val source = scala.io.Source.fromFile(path)
      try {
        val content = source.getLines().mkString("\n")
        fromYaml(content) match {
          case Right(config) => config
          case Left(error) => throw new RuntimeException(s"Failed to parse YAML: $error")
        }
      } finally {
        source.close()
      }
    }.toEither
  }
  
  def fromYamlSchema(yamlString: String): Either[Error, RankingConfigSchema] = {
    parser.parse(yamlString).flatMap(_.as[RankingConfigSchema])
  }
  
  def fromYamlSchemaFile(path: String): Either[Throwable, RankingConfigSchema] = {
    scala.util.Try {
      val source = scala.io.Source.fromFile(path)
      try {
        val content = source.getLines().mkString("\n")
        fromYamlSchema(content) match {
          case Right(config) => config
          case Left(error) => throw new RuntimeException(s"Failed to parse YAML: $error")
        }
      } finally {
        source.close()
      }
    }.toEither
  }
}