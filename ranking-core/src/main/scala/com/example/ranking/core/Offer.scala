package com.example.ranking.core

case class Offer(
  id: String,
  features: Map[String, Double],
  metadata: Map[String, Any] = Map.empty
)

case class ScoredOffer(
  offer: Offer,
  score: Double,
  appliedFilters: List[String] = List.empty,
  appliedBoosts: Map[String, Double] = Map.empty,
  debugInfo: Map[String, Any] = Map.empty
)

object Offer {
  def fromItem(item: Item): Offer = {
    val features = item.metadata.view.mapValues { value: Any =>
      value match {
        case d: Double => d
        case s: String => s.toDoubleOption.getOrElse(0.0)
        case i: Int => i.toDouble
        case f: Float => f.toDouble
        case _ => 0.0
      }
    }.toMap + ("base_score" -> item.score)
    
    Offer(
      id = item.id,
      features = features,
      metadata = item.metadata.view.mapValues(identity).toMap
    )
  }
  
  implicit class OfferOps(offer: Offer) {
    def withFeature(key: String, value: Double): Offer = 
      offer.copy(features = offer.features + (key -> value))
      
    def withMetadata(key: String, value: Any): Offer = 
      offer.copy(metadata = offer.metadata + (key -> value))
      
    def hasFeature(key: String): Boolean = offer.features.contains(key)
    
    def getFeature(key: String): Option[Double] = offer.features.get(key)
    
    def getMetadataString(key: String): Option[String] = 
      offer.metadata.get(key).map(_.toString)
      
    def getMetadataDouble(key: String): Option[Double] = 
      offer.metadata.get(key).flatMap {
        case d: Double => Some(d)
        case s: String => s.toDoubleOption
        case i: Int => Some(i.toDouble)
        case f: Float => Some(f.toDouble)
        case _ => None
      }
  }
}