package io.github.spritzsn.spritz

import org.scalatest.funsuite.AnyFunSuite

class RouteParserTests extends AnyFunSuite:
  test("root path") {
    assert(RouteParser("/") == RouteAST.Slash)
  }

  test("simple path") {
    val ast = RouteParser("/hello")
    assert(ast == RouteAST.Sequence(List(RouteAST.Slash, RouteAST.Literal("hello"))))
  }

  test("multi-segment path") {
    val ast = RouteParser("/api/users")
    assert(ast == RouteAST.Sequence(List(
      RouteAST.Sequence(List(RouteAST.Slash, RouteAST.Literal("api"))),
      RouteAST.Sequence(List(RouteAST.Slash, RouteAST.Literal("users"))),
    )))
  }

  test("path with parameter") {
    val ast = RouteParser("/users/:id")
    assert(ast == RouteAST.Sequence(List(
      RouteAST.Sequence(List(RouteAST.Slash, RouteAST.Literal("users"))),
      RouteAST.Sequence(List(RouteAST.Slash, RouteAST.Parameter("id"))),
    )))
  }

  test("path with multiple parameters") {
    val ast = RouteParser("/users/:userId/posts/:postId")
    assert(ast == RouteAST.Sequence(List(
      RouteAST.Sequence(List(RouteAST.Slash, RouteAST.Literal("users"))),
      RouteAST.Sequence(List(RouteAST.Slash, RouteAST.Parameter("userId"))),
      RouteAST.Sequence(List(RouteAST.Slash, RouteAST.Literal("posts"))),
      RouteAST.Sequence(List(RouteAST.Slash, RouteAST.Parameter("postId"))),
    )))
  }

  test("path with literal and parameter mixed in segment") {
    val ast = RouteParser("/file-:name")
    assert(ast == RouteAST.Sequence(List(
      RouteAST.Slash, RouteAST.Literal("file-"), RouteAST.Parameter("name"),
    )))
  }
