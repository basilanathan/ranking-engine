package com.example.ranking.core

import fastparse._
import NoWhitespace._

object ExpressionParser {
  
  def number[_: P]: P[Number] = P(
    (CharIn("0-9").rep(1) ~ ("." ~ CharIn("0-9").rep(1)).?).!.map(_.toDouble) |
    ("." ~ CharIn("0-9").rep(1)).!.map(_.toDouble)
  ).map(Number(_))

  def variable[_: P]: P[Variable] = P(
    CharIn("a-zA-Z_") ~ CharIn("a-zA-Z0-9_").rep
  ).!.map(Variable(_))

  def functionCall[_: P]: P[FunctionCall] = P(
    (CharIn("a-zA-Z_") ~ CharIn("a-zA-Z0-9_").rep).! ~ "(" ~ " ".rep ~ expression.rep(sep = " ".rep ~ "," ~ " ".rep) ~ " ".rep ~ ")"
  ).map { case (name, args) => FunctionCall(name, args.toList) }

  def factor[_: P]: P[Expr] = P(
    functionCall | 
    number | 
    variable | 
    ("(" ~ " ".rep ~ expression ~ " ".rep ~ ")")
  )

  def unary[_: P]: P[Expr] = P(
    (CharIn("+\\-").! ~ " ".rep ~ unary).map { case (op, expr) => UnaryOp(op, expr) } |
    factor
  )

  def power[_: P]: P[Expr] = P(
    unary ~ (" ".rep ~ ("^" | "**").! ~ " ".rep ~ unary).rep
  ).map { case (left, ops) =>
    ops.foldRight(left) { case ((op, right), acc) => BinaryOp(acc, op, right) }
  }

  def term[_: P]: P[Expr] = P(
    power ~ (" ".rep ~ CharIn("*/").! ~ " ".rep ~ power).rep
  ).map { case (left, ops) =>
    ops.foldLeft(left) { case (acc, (op, right)) => BinaryOp(acc, op, right) }
  }

  def expression[_: P]: P[Expr] = P(
    term ~ (" ".rep ~ CharIn("+\\-").! ~ " ".rep ~ term).rep
  ).map { case (left, ops) =>
    ops.foldLeft(left) { case (acc, (op, right)) => BinaryOp(acc, op, right) }
  }

  def parseExpression[_: P]: P[Expr] = P(Start ~ " ".rep ~ expression ~ " ".rep ~ End)

  def parse(input: String): Either[String, Expr] = {
    fastparse.parse(input, parseExpression(_)) match {
      case Parsed.Success(expr, _) => Right(expr)
      case failure: Parsed.Failure => Left(s"Parse error: ${failure.msg}")
    }
  }
}