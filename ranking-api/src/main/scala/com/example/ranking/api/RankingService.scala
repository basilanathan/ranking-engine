package com.example.ranking.api

import com.example.ranking.core._
import com.example.ranking.config._
import io.circe._
import io.circe.generic.semiauto._

case class RankingRequest(items: List[Item], config: Option[RankingConfig] = None)
case class RankingResponse(rankedItems: List[Item])

object RankingService {
  implicit val itemDecoder: Decoder[Item] = deriveDecoder[Item]
  implicit val itemEncoder: Encoder[Item] = deriveEncoder[Item]
  implicit val requestDecoder: Decoder[RankingRequest] = deriveDecoder[RankingRequest]
  implicit val requestEncoder: Encoder[RankingRequest] = deriveEncoder[RankingRequest]
  implicit val responseDecoder: Decoder[RankingResponse] = deriveDecoder[RankingResponse]
  implicit val responseEncoder: Encoder[RankingResponse] = deriveEncoder[RankingResponse]
  
  def rank(request: RankingRequest): Either[String, RankingResponse] = {
    val strategy = request.config match {
      case Some(config) => createStrategyFromConfig(config)
      case None => Right(RankingStrategies.scoreBasedRanking)
    }
    
    strategy.map { strat =>
      val engine = RankingEngine(strat)
      val rankedItems = engine.rank(request.items)
      RankingResponse(rankedItems)
    }
  }
  
  private def createStrategyFromConfig(config: RankingConfig): Either[String, RankingStrategy[Item]] = {
    ConfigValidator.validate(config) match {
      case Left(errors) => Left(s"Configuration validation failed: ${errors.mkString(", ")}")
      case Right(validConfig) => 
        validConfig.strategy match {
          case "score-based" => Right(RankingStrategies.scoreBasedRanking)
          case "weighted" => Right(RankingStrategies.weightedRanking(validConfig.weights))
          case other => Left(s"Unsupported strategy: $other")
        }
    }
  }
}