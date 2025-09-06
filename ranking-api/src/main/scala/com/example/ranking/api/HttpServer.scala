package com.example.ranking.api

import cats.effect._
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.ember.server._
import com.comcast.ip4s._
import RankingService._

object HttpServer extends IOApp {
  
  val rankingRoutes = HttpRoutes.of[IO] {
    case GET -> Root / "health" =>
      Ok("Ranking Engine is running")
      
    case req @ POST -> Root / "rank" =>
      for {
        request <- req.as[RankingRequest]
        response <- RankingService.rank(request) match {
          case Right(result) => Ok(result)
          case Left(error) => BadRequest(error)
        }
      } yield response
  }
  
  val app = rankingRoutes.orNotFound
  
  def run(args: List[String]): IO[ExitCode] = {
    EmberServerBuilder
      .default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8080")
      .withHttpApp(app)
      .build
      .use(_ => IO.never)
      .as(ExitCode.Success)
  }
}