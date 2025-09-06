package com.example.ranking.core

import cats.implicits._

class RankingEngine[T](strategy: RankingStrategy[T]) {
  def rank(items: List[T]): List[T] = strategy.rank(items)
}

object RankingEngine {
  def apply[T](strategy: RankingStrategy[T]): RankingEngine[T] = new RankingEngine(strategy)
  
  def withDefaultStrategy: RankingEngine[Item] = 
    new RankingEngine(RankingStrategies.scoreBasedRanking)
}