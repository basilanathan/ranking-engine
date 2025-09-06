package com.example.ranking.tests

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import com.example.ranking.core.ExpressionEvaluator
import com.example.ranking.core.ExpressionEvaluator._

class ExpressionEvaluatorSpec extends AnyFlatSpec with Matchers {

  val features = Map(
    "revenue" -> 1.2,
    "engagement" -> 0.8,
    "risk_score" -> 0.3,
    "popularity" -> 2.5
  )

  "ExpressionEvaluator" should "evaluate simple arithmetic expressions" in {
    val result = "0.6 * revenue + 0.4 * engagement".evaluate(features)
    result shouldBe a [Right[_, _]]
    result.toOption.get should be (0.6 * 1.2 + 0.4 * 0.8 +- 0.001)
  }

  it should "handle subtraction and division" in {
    val result = "revenue - risk_score / 2".evaluate(features)
    result shouldBe a [Right[_, _]]
    result.toOption.get should be (1.2 - 0.3 / 2.0 +- 0.001)
  }

  it should "respect operator precedence" in {
    val result1 = "2 + 3 * 4".evaluate(Map.empty)
    val result2 = "(2 + 3) * 4".evaluate(Map.empty)
    
    result1.toOption.get should be (14.0)
    result2.toOption.get should be (20.0)
  }

  it should "handle unary operators" in {
    val result1 = "-revenue + engagement".evaluate(features)
    val result2 = "+revenue - engagement".evaluate(features)
    
    result1.toOption.get should be (-1.2 + 0.8 +- 0.001)
    result2.toOption.get should be (1.2 - 0.8 +- 0.001)
  }

  it should "handle power operations" in {
    val result1 = "2 ^ 3".evaluate(Map.empty)
    val result2 = "revenue ** 2".evaluate(features)
    
    result1.toOption.get should be (8.0)
    result2.toOption.get should be (1.44 +- 0.001)
  }

  it should "evaluate mathematical functions" in {
    val testFeatures = Map("x" -> 9.0, "y" -> -5.0, "z" -> 2.0)
    
    "abs(y)".evaluate(testFeatures).toOption.get should be (5.0)
    "sqrt(x)".evaluate(testFeatures).toOption.get should be (3.0)
    "max(x, y, z)".evaluate(testFeatures).toOption.get should be (9.0)
    "min(x, y, z)".evaluate(testFeatures).toOption.get should be (-5.0)
  }

  it should "handle complex nested expressions" in {
    val expression = "0.6 * revenue + 0.4 * engagement - 0.1 * risk_score"
    val result = expression.evaluate(features)
    
    val expected = 0.6 * 1.2 + 0.4 * 0.8 - 0.1 * 0.3
    result.toOption.get should be (expected +- 0.001)
  }

  it should "return error for missing variables" in {
    val result = "unknown_var * 2".evaluate(features)
    result shouldBe a [Left[_, _]]
    result.swap.toOption.get should include ("Variable 'unknown_var' not found")
  }

  it should "return error for division by zero" in {
    val result = "revenue / 0".evaluate(features)
    result shouldBe a [Left[_, _]]
    result.swap.toOption.get should include ("Division by zero")
  }

  it should "return error for invalid function calls" in {
    val result1 = "sqrt(-1)".evaluate(features)
    val result2 = "unknown_function(1)".evaluate(features)
    
    result1 shouldBe a [Left[_, _]]
    result2 shouldBe a [Left[_, _]]
  }

  it should "extract variables from expressions" in {
    val vars1 = "0.6 * revenue + 0.4 * engagement".variables
    val vars2 = "sqrt(popularity) - risk_score / 2".variables
    
    vars1.toOption.get should contain theSameElementsAs Set("revenue", "engagement")
    vars2.toOption.get should contain theSameElementsAs Set("popularity", "risk_score")
  }

  it should "validate expression syntax" in {
    "revenue + engagement".isValid should be (true)
    "revenue +".isValid should be (false)
    "* engagement".isValid should be (false)
    "sqrt(revenue)".isValid should be (true)
  }

  it should "provide detailed evaluation info" in {
    val allFeatures = Map("revenue" -> 1.0, "engagement" -> 2.0, "unused" -> 3.0)
    val result = ExpressionEvaluator.evaluateWithInfo("revenue + engagement", allFeatures)
    
    result shouldBe a [Right[_, _]]
    val info = result.toOption.get
    info.value should be (3.0)
    info.usedVariables should contain theSameElementsAs Set("revenue", "engagement")
    info.unusedVariables should contain ("unused")
  }

  it should "handle expressions with defaults" in {
    val defaults = Map("base_score" -> 1.0, "multiplier" -> 2.0)
    val userFeatures = Map("revenue" -> 5.0)
    
    val result = ExpressionEvaluator.evaluateWithDefaults(
      "base_score + multiplier * revenue", 
      userFeatures, 
      defaults
    )
    
    result.toOption.get should be (1.0 + 2.0 * 5.0)
  }

  it should "allow user features to override defaults" in {
    val defaults = Map("multiplier" -> 1.0)
    val userFeatures = Map("multiplier" -> 3.0, "revenue" -> 2.0)
    
    val result = ExpressionEvaluator.evaluateWithDefaults(
      "multiplier * revenue", 
      userFeatures, 
      defaults
    )
    
    result.toOption.get should be (3.0 * 2.0) // Should use user's multiplier, not default
  }

  "String extension methods" should "work correctly" in {
    import ExpressionEvaluator.ExpressionStringOps
    
    val expr = "revenue * 2 + engagement"
    expr.evaluate(features).toOption.get should be (1.2 * 2 + 0.8 +- 0.001)
    expr.variables.toOption.get should contain theSameElementsAs Set("revenue", "engagement")
    expr.isValid should be (true)
  }
}