package io.github.spritzsn.spritz

import scala.util.matching.Regex
import scala.util.parsing.combinator.RegexParsers
import scala.util.parsing.input.{PagedSeq, PagedSeqReader}

object RouteParser extends RegexParsers:

  override val skipWhitespace = false

  def route: Parser[RouteAST] =
    rep1(segment) ^^ {
      case List(s) => s
      case l       => RouteAST.Sequence(l)
    }

  def segment: Parser[RouteAST] =
    "/" ~ rep(piece) ^^ {
      case _ ~ Nil => RouteAST.Slash
      case _ ~ ps  => RouteAST.Sequence(RouteAST.Slash +: ps)
    }

  def piece: Parser[RouteAST] = parameter | literal

  def literal: Parser[RouteAST] = "[a-zA-Z0-9-_.]+".r ^^ RouteAST.Literal.apply

  def parameter: Parser[RouteAST] = ":[a-zA-Z0-9]+".r ^^ (n => RouteAST.Parameter(n drop 1))

  def apply(input: scala.io.Source): RouteAST =
    parseAll(route, new PagedSeqReader(PagedSeq.fromSource(input))) match {
      case Success(result, _) => result
      case Error(msg, next)   => sys.error(s"$msg: ${next.pos.longString}")
      case Failure(msg, next) => sys.error(s"$msg: ${next.pos.longString}")
    }

  def apply(input: String): RouteAST = apply(scala.io.Source.fromString(input))
