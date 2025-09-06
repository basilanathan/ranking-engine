package com.example.ranking.core

case class ResolvedRankingConfig(
  rankingStrategy: String,
  scoringFormula: String,
  filters: List[FilterCondition],
  maxResults: Int,
  boostFactors: Map[String, Double],
  experimentGroups: Map[String, ExperimentGroupConfig] = Map.empty
)

case class RankingResult(
  rankedOffers: List[ScoredOffer],
  totalProcessed: Int,
  totalFiltered: Int,
  experimentOverride: Option[ExperimentOverride] = None,
  debugInfo: Map[String, Any] = Map.empty
)

class Ranker(config: ResolvedRankingConfig) {
  
  def rank(offers: List[Offer]): Either[String, RankingResult] = {
    rankWithDetails(offers, RankingContext.empty, includeDebugInfo = false)
  }
  
  def rank(offers: List[Offer], context: RankingContext): Either[String, RankingResult] = {
    rankWithDetails(offers, context, includeDebugInfo = false)
  }
  
  def rankWithDetails(offers: List[Offer], includeDebugInfo: Boolean): Either[String, RankingResult] = {
    rankWithDetails(offers, RankingContext.empty, includeDebugInfo)
  }
  
  def rankWithDetails(
    offers: List[Offer], 
    context: RankingContext, 
    includeDebugInfo: Boolean = true
  ): Either[String, RankingResult] = {
    val startTime = System.currentTimeMillis()
    
    // First resolve experiment overrides
    ExperimentResolver.resolveConfig(config, context) match {
      case Left(error) => Left(error)
      case Right((effectiveConfig, experimentOverride)) =>
        val results = for {
          filteredOffers <- applyFilters(offers, effectiveConfig)
          scoredOffers <- scoreOffers(filteredOffers, effectiveConfig, includeDebugInfo)
          rankedOffers = sortOffers(scoredOffers)
          limitedOffers = applyResultLimit(rankedOffers, effectiveConfig)
        } yield {
      val processingTime = System.currentTimeMillis() - startTime
      val debugInfo = if (includeDebugInfo) {
        Map(
          "processingTimeMs" -> processingTime,
          "scoringFormula" -> effectiveConfig.scoringFormula,
          "filtersApplied" -> effectiveConfig.filters.size,
          "boostFactorsUsed" -> effectiveConfig.boostFactors.keys.toList,
          "resultLimit" -> effectiveConfig.maxResults,
          "experimentGroup" -> context.experimentGroup,
          "experimentOverrides" -> experimentOverride.map(_.appliedOverrides)
        )
      } else Map.empty[String, Any]
      
      RankingResult(
        rankedOffers = limitedOffers,
        totalProcessed = offers.size,
        totalFiltered = offers.size - filteredOffers.size,
        experimentOverride = experimentOverride,
        debugInfo = debugInfo
      )
    }
    
    results
    }
  }
  
  private def applyFilters(offers: List[Offer], effectiveConfig: ResolvedRankingConfig): Either[String, List[Offer]] = {
    if (effectiveConfig.filters.isEmpty) {
      Right(offers)
    } else {
      val filteredResults = offers.map { offer =>
        FilterEvaluator.evaluateFilters(offer, effectiveConfig.filters).map { filterResult =>
          if (filterResult.passedAll) Some(offer) else None
        }
      }
      
      // Check for filter evaluation errors
      val errors = filteredResults.collect { case Left(error) => error }
      if (errors.nonEmpty) {
        Left(s"Filter evaluation failed: ${errors.mkString("; ")}")
      } else {
        val passed = filteredResults.collect { 
          case Right(Some(offer)) => offer 
        }
        Right(passed)
      }
    }
  }
  
  private def scoreOffers(offers: List[Offer], effectiveConfig: ResolvedRankingConfig, includeDebugInfo: Boolean): Either[String, List[ScoredOffer]] = {
    val scoringResults = offers.map { offer =>
      scoreOffer(offer, effectiveConfig, includeDebugInfo)
    }
    
    // Check for scoring errors
    val errors = scoringResults.collect { case Left(error) => error }
    if (errors.nonEmpty) {
      Left(s"Scoring failed: ${errors.mkString("; ")}")
    } else {
      val scored = scoringResults.collect { case Right(scoredOffer) => scoredOffer }
      Right(scored)
    }
  }
  
  private def scoreOffer(offer: Offer, effectiveConfig: ResolvedRankingConfig, includeDebugInfo: Boolean): Either[String, ScoredOffer] = {
    for {
      baseScore <- ExpressionEvaluator.evaluate(effectiveConfig.scoringFormula, offer.features)
      boostedScore = applyBoosts(offer, baseScore, effectiveConfig)
    } yield {
      val appliedBoosts = calculateAppliedBoosts(offer, effectiveConfig)
      val debugInfo = if (includeDebugInfo) {
        Map(
          "baseScore" -> baseScore,
          "boostedScore" -> boostedScore,
          "featuresUsed" -> offer.features.keys.toList,
          "boostMultiplier" -> (if (baseScore != 0) boostedScore / baseScore else 1.0)
        )
      } else Map.empty[String, Any]
      
      ScoredOffer(
        offer = offer,
        score = boostedScore,
        appliedBoosts = appliedBoosts,
        debugInfo = debugInfo
      )
    }
  }
  
  private def applyBoosts(offer: Offer, baseScore: Double, effectiveConfig: ResolvedRankingConfig): Double = {
    val multiplier = effectiveConfig.boostFactors.foldLeft(1.0) { case (acc, (boostName, factor)) =>
      if (shouldApplyBoost(offer, boostName)) {
        acc * factor
      } else {
        acc
      }
    }
    
    baseScore * multiplier
  }
  
  private def calculateAppliedBoosts(offer: Offer, effectiveConfig: ResolvedRankingConfig): Map[String, Double] = {
    effectiveConfig.boostFactors.view.mapValues { factor =>
      if (shouldApplyBoost(offer, "")) factor else 1.0
    }.toMap
  }
  
  private def shouldApplyBoost(offer: Offer, boostName: String): Boolean = {
    // Boost application logic - can be customized
    boostName.toLowerCase match {
      case "premium" => 
        offer.metadata.get("tier").contains("premium") ||
        offer.getMetadataDouble("revenue").exists(_ > 10.0)
      
      case "trending" =>
        offer.metadata.get("trending").contains(true) ||
        offer.metadata.get("trending").contains("true") ||
        offer.getMetadataDouble("engagement").exists(_ > 1.5)
      
      case "new_offers" | "new" =>
        offer.metadata.get("isNew").contains(true) ||
        offer.metadata.get("isNew").contains("true") ||
        offer.metadata.get("age_days").flatMap(_.toString.toIntOption).exists(_ < 7)
        
      case "personalized" =>
        offer.metadata.get("personalized").contains(true) ||
        offer.getMetadataDouble("user_affinity").exists(_ > 0.8)
      
      case _ =>
        // Default: check if boost name exists as metadata key
        offer.metadata.get(boostName).contains(true) ||
        offer.metadata.get(boostName).contains("true") ||
        offer.features.get(s"${boostName}_eligible").exists(_ > 0.5)
    }
  }
  
  private def sortOffers(offers: List[ScoredOffer]): List[ScoredOffer] = {
    offers.sortBy(_.score)(Ordering[Double].reverse)
  }
  
  private def applyResultLimit(offers: List[ScoredOffer], effectiveConfig: ResolvedRankingConfig): List[ScoredOffer] = {
    offers.take(effectiveConfig.maxResults)
  }
}

object Ranker {
  
  def fromConfig(config: ResolvedRankingConfig): Ranker = {
    new Ranker(config)
  }
  
  def validateConfig(
    config: ResolvedRankingConfig,
    sampleFeatures: Map[String, Double] = Map.empty
  ): Either[String, Unit] = {
    if (sampleFeatures.nonEmpty) {
      ExpressionEvaluator.evaluate(config.scoringFormula, sampleFeatures) match {
        case Left(error) => Left(s"Scoring formula validation failed: $error")
        case Right(_) => Right(())
      }
    } else {
      ExpressionEvaluator.validateExpression(config.scoringFormula) match {
        case Left(error) => Left(s"Scoring formula syntax error: $error")
        case Right(_) => Right(())
      }
    }
  }
  
  // Convenience method for quick ranking without detailed configuration
  def quickRank(
    offers: List[Offer], 
    formula: String, 
    maxResults: Int = 100
  ): Either[String, List[ScoredOffer]] = {
    val config = ResolvedRankingConfig(
      rankingStrategy = "expression_based",
      scoringFormula = formula,
      filters = List.empty,
      maxResults = maxResults,
      boostFactors = Map.empty,
      experimentGroups = Map.empty
    )
    
    val ranker = fromConfig(config)
    ranker.rank(offers).map(_.rankedOffers)
  }
  
  // Convenience method for quick ranking with experiment context
  def quickRankWithExperiment(
    offers: List[Offer], 
    formula: String, 
    context: RankingContext,
    maxResults: Int = 100
  ): Either[String, List[ScoredOffer]] = {
    val config = ResolvedRankingConfig(
      rankingStrategy = "expression_based",
      scoringFormula = formula,
      filters = List.empty,
      maxResults = maxResults,
      boostFactors = Map.empty,
      experimentGroups = Map.empty
    )
    
    val ranker = fromConfig(config)
    ranker.rank(offers, context).map(_.rankedOffers)
  }
}