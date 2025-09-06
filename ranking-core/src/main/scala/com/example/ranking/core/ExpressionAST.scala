package com.example.ranking.core

sealed trait Expr {
  def evaluate(features: Map[String, Double]): Either[String, Double]
}

case class Number(value: Double) extends Expr {
  def evaluate(features: Map[String, Double]): Either[String, Double] = Right(value)
}

case class Variable(name: String) extends Expr {
  def evaluate(features: Map[String, Double]): Either[String, Double] = {
    features.get(name) match {
      case Some(value) => Right(value)
      case None => Left(s"Variable '$name' not found in features")
    }
  }
}

case class BinaryOp(left: Expr, operator: String, right: Expr) extends Expr {
  def evaluate(features: Map[String, Double]): Either[String, Double] = {
    for {
      leftValue <- left.evaluate(features)
      rightValue <- right.evaluate(features)
      result <- operator match {
        case "+" => Right(leftValue + rightValue)
        case "-" => Right(leftValue - rightValue)
        case "*" => Right(leftValue * rightValue)
        case "/" => 
          if (rightValue == 0.0) Left("Division by zero")
          else Right(leftValue / rightValue)
        case "^" | "**" => Right(math.pow(leftValue, rightValue))
        case _ => Left(s"Unknown operator: $operator")
      }
    } yield result
  }
}

case class UnaryOp(operator: String, operand: Expr) extends Expr {
  def evaluate(features: Map[String, Double]): Either[String, Double] = {
    operand.evaluate(features).flatMap { value =>
      operator match {
        case "+" => Right(value)
        case "-" => Right(-value)
        case _ => Left(s"Unknown unary operator: $operator")
      }
    }
  }
}

case class FunctionCall(name: String, args: List[Expr]) extends Expr {
  import ExprOps._
  
  def evaluate(features: Map[String, Double]): Either[String, Double] = {
    val evaluatedArgs = args.map(_.evaluate(features)).sequence
    
    evaluatedArgs.flatMap { argValues =>
      name.toLowerCase match {
        case "abs" => 
          if (argValues.length != 1) Left("abs() requires exactly 1 argument")
          else Right(math.abs(argValues.head))
        case "sqrt" => 
          if (argValues.length != 1) Left("sqrt() requires exactly 1 argument")
          else if (argValues.head < 0) Left("sqrt() of negative number")
          else Right(math.sqrt(argValues.head))
        case "log" => 
          if (argValues.length != 1) Left("log() requires exactly 1 argument")
          else if (argValues.head <= 0) Left("log() of non-positive number")
          else Right(math.log(argValues.head))
        case "sin" => 
          if (argValues.length != 1) Left("sin() requires exactly 1 argument")
          else Right(math.sin(argValues.head))
        case "cos" => 
          if (argValues.length != 1) Left("cos() requires exactly 1 argument")
          else Right(math.cos(argValues.head))
        case "max" => 
          if (argValues.isEmpty) Left("max() requires at least 1 argument")
          else Right(argValues.max)
        case "min" => 
          if (argValues.isEmpty) Left("min() requires at least 1 argument")
          else Right(argValues.min)
        case _ => Left(s"Unknown function: $name")
      }
    }
  }
}

object ExprOps {
  implicit class ListEitherOps[A, B](list: List[Either[A, B]]) {
    def sequence: Either[A, List[B]] = {
      list.foldRight(Right(Nil): Either[A, List[B]]) { (either, acc) =>
        for {
          value <- either
          list <- acc
        } yield value :: list
      }
    }
  }
}