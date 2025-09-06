package com.example.ranking.core

case class RankingContext(
  experimentGroup: Option[String] = None,
  userId: Option[String] = None,
  sessionId: Option[String] = None,
  requestId: Option[String] = None,
  metadata: Map[String, Any] = Map.empty
)

object RankingContext {
  val empty: RankingContext = RankingContext()
  
  def withExperiment(experimentGroup: String): RankingContext = 
    RankingContext(experimentGroup = Some(experimentGroup))
    
  def withUser(userId: String): RankingContext = 
    RankingContext(userId = Some(userId))
    
  def withSession(sessionId: String): RankingContext = 
    RankingContext(sessionId = Some(sessionId))
    
  implicit class RankingContextOps(context: RankingContext) {
    def withExperiment(experimentGroup: String): RankingContext = 
      context.copy(experimentGroup = Some(experimentGroup))
      
    def withUser(userId: String): RankingContext = 
      context.copy(userId = Some(userId))
      
    def withSession(sessionId: String): RankingContext = 
      context.copy(sessionId = Some(sessionId))
      
    def withMetadata(key: String, value: Any): RankingContext = 
      context.copy(metadata = context.metadata + (key -> value))
      
    def hasExperiment: Boolean = context.experimentGroup.isDefined
    
    def isInExperiment(experimentName: String): Boolean = 
      context.experimentGroup.contains(experimentName)
  }
}