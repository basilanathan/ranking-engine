package com.example.ranking.core

trait RankingStrategy[T] {
  def rank(items: List[T]): List[T]
}

case class Item(id: String, score: Double, metadata: Map[String, String] = Map.empty)

object RankingStrategies {
  implicit val scoreBasedRanking: RankingStrategy[Item] = new RankingStrategy[Item] {
    def rank(items: List[Item]): List[Item] = items.sortBy(-_.score)
  }
  
  def weightedRanking(weights: Map[String, Double]): RankingStrategy[Item] = 
    new RankingStrategy[Item] {
      def rank(items: List[Item]): List[Item] = {
        items.map { item =>
          val weightedScore = weights.foldLeft(0.0) { case (acc, (key, weight)) =>
            item.metadata.get(key).flatMap(_.toDoubleOption).map(_ * weight).getOrElse(0.0) + acc
          }
          item.copy(score = item.score + weightedScore)
        }.sortBy(-_.score)
      }
    }
}