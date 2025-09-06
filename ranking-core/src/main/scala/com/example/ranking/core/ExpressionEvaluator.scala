package com.example.ranking.core

object ExpressionEvaluator {
  
  def evaluate(expression: String, features: Map[String, Double]): Either[String, Double] = {
    for {
      ast <- ExpressionParser.parse(expression)
      result <- ast.evaluate(features)
    } yield result
  }
  
  def evaluateWithDefaults(
    expression: String, 
    features: Map[String, Double], 
    defaults: Map[String, Double] = Map.empty
  ): Either[String, Double] = {
    val combinedFeatures = defaults ++ features
    evaluate(expression, combinedFeatures)
  }
  
  def validateExpression(expression: String): Either[String, Unit] = {
    ExpressionParser.parse(expression).map(_ => ())
  }
  
  def extractVariables(expression: String): Either[String, Set[String]] = {
    ExpressionParser.parse(expression).map(extractVariablesFromAST)
  }
  
  private def extractVariablesFromAST(expr: Expr): Set[String] = expr match {
    case Number(_) => Set.empty
    case Variable(name) => Set(name)
    case BinaryOp(left, _, right) => extractVariablesFromAST(left) ++ extractVariablesFromAST(right)
    case UnaryOp(_, operand) => extractVariablesFromAST(operand)
    case FunctionCall(_, args) => args.flatMap(extractVariablesFromAST).toSet
  }
  
  case class EvaluationResult(
    value: Double,
    usedVariables: Set[String],
    unusedVariables: Set[String]
  )
  
  def evaluateWithInfo(
    expression: String, 
    features: Map[String, Double]
  ): Either[String, EvaluationResult] = {
    for {
      ast <- ExpressionParser.parse(expression)
      requiredVars = extractVariablesFromAST(ast)
      result <- ast.evaluate(features)
      usedVars = requiredVars.intersect(features.keySet)
      unusedVars = features.keySet -- requiredVars
    } yield EvaluationResult(result, usedVars, unusedVars)
  }
  
  implicit class ExpressionStringOps(expression: String) {
    def evaluate(features: Map[String, Double]): Either[String, Double] = 
      ExpressionEvaluator.evaluate(expression, features)
      
    def variables: Either[String, Set[String]] = 
      ExpressionEvaluator.extractVariables(expression)
      
    def isValid: Boolean = 
      ExpressionEvaluator.validateExpression(expression).isRight
  }
}