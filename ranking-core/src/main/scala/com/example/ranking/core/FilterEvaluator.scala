package com.example.ranking.core

case class FilterCondition(
  condition: String,
  description: Option[String] = None
)

object FilterEvaluator {
  
  def evaluateFilters(
    offer: Offer, 
    filters: List[FilterCondition]
  ): Either[String, FilterResult] = {
    
    val results = filters.map { filter =>
      evaluateFilter(offer, filter).map(passed => FilterApplication(filter, passed))
    }
    
    // Check if any filter evaluation failed
    val failures = results.collect { case Left(error) => error }
    if (failures.nonEmpty) {
      Left(s"Filter evaluation errors: ${failures.mkString(", ")}")
    } else {
      val applications = results.collect { case Right(app) => app }
      val passedAll = applications.forall(_.passed)
      val appliedFilters = applications.filter(_.passed).map(app => app.filter.description.getOrElse(app.filter.condition))
      
      Right(FilterResult(passedAll, appliedFilters))
    }
  }
  
  private def evaluateFilter(offer: Offer, filter: FilterCondition): Either[String, Boolean] = {
    val condition = filter.condition.trim
    
    // Parse simple comparison conditions
    parseCondition(condition) match {
      case Right((variable, operator, value)) =>
        evaluateComparison(offer, variable, operator, value)
      case Left(_) =>
        // Try to evaluate as a boolean expression using ExpressionEvaluator
        evaluateAsExpression(offer, condition)
    }
  }
  
  private def parseCondition(condition: String): Either[String, (String, String, String)] = {
    val operators = List("==", "!=", ">=", "<=", ">", "<")
    
    operators.collectFirst { op =>
      if (condition.contains(op)) {
        val parts = condition.split(op, 2).map(_.trim)
        if (parts.length == 2) {
          Some((parts(0), op, parts(1)))
        } else None
      } else None
    }.flatten match {
      case Some(result) => Right(result)
      case None => Left(s"Could not parse condition: $condition")
    }
  }
  
  private def evaluateComparison(
    offer: Offer, 
    variable: String, 
    operator: String, 
    expectedValue: String
  ): Either[String, Boolean] = {
    
    val actualValue = resolveValue(offer, variable)
    val expected = parseValue(expectedValue)
    
    (actualValue, expected) match {
      case (Some(actual), Some(exp)) =>
        val result = operator match {
          case "==" => math.abs(actual - exp) < 1e-10
          case "!=" => math.abs(actual - exp) >= 1e-10
          case ">" => actual > exp
          case "<" => actual < exp
          case ">=" => actual >= exp
          case "<=" => actual <= exp
          case _ => false
        }
        Right(result)
        
      case (Some(actual), None) =>
        // String comparison for non-numeric expected values
        val actualStr = actual.toString
        val result = operator match {
          case "==" => actualStr == expectedValue.replaceAll("\"", "").replaceAll("'", "")
          case "!=" => actualStr != expectedValue.replaceAll("\"", "").replaceAll("'", "")
          case _ => false
        }
        Right(result)
        
      case (None, _) =>
        // Check metadata for string values
        val metadataValue = offer.metadata.get(variable).map(_.toString)
        val cleanExpected = expectedValue.replaceAll("\"", "").replaceAll("'", "")
        
        metadataValue match {
          case Some(value) =>
            val result = operator match {
              case "==" => value == cleanExpected
              case "!=" => value != cleanExpected
              case _ => false
            }
            Right(result)
          case None => Left(s"Variable '$variable' not found in offer features or metadata")
        }
    }
  }
  
  private def evaluateAsExpression(offer: Offer, condition: String): Either[String, Boolean] = {
    // Replace metadata references in the condition
    val expandedCondition = expandMetadataReferences(offer, condition)
    
    ExpressionEvaluator.evaluate(expandedCondition, offer.features).map { result =>
      // Treat non-zero as true, zero as false
      math.abs(result) > 1e-10
    }
  }
  
  private def expandMetadataReferences(offer: Offer, condition: String): String = {
    // Simple replacement for common patterns like "offer.category == 'shopping'"
    var expanded = condition
    
    // Replace offer.field patterns
    val offerPattern = """offer\.(\w+)""".r
    expanded = offerPattern.replaceAllIn(expanded, m => {
      val field = m.group(1)
      offer.metadata.get(field) match {
        case Some(value: String) => s"'$value'"
        case Some(value: Double) => value.toString
        case Some(value: Int) => value.toString
        case Some(value) => s"'${value.toString}'"
        case None => "null"
      }
    })
    
    expanded
  }
  
  private def resolveValue(offer: Offer, variable: String): Option[Double] = {
    offer.features.get(variable)
      .orElse(offer.getMetadataDouble(variable))
  }
  
  private def parseValue(value: String): Option[Double] = {
    val cleaned = value.replaceAll("\"", "").replaceAll("'", "")
    cleaned.toDoubleOption
  }
}

case class FilterApplication(filter: FilterCondition, passed: Boolean)
case class FilterResult(passedAll: Boolean, appliedFilters: List[String])