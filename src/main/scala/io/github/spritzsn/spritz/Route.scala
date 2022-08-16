package io.github.spritzsn.spritz

import scala.concurrent.Future
import scala.util.matching.Regex

type EndpointHandler = (Request, Response) => Any

type MiddlewareHandler = (Request, Response) => HandlerResult

type Method = "GET" | "POST" | "PUT" | "DELETE" | "PATCH"

enum Route:
  case Endpoint(method: Method, path: Regex, params: Seq[String], handler: EndpointHandler) extends Route
  case PathRoutes(path: Regex, params: Seq[String], handler: MiddlewareHandler) extends Route
  case Middleware(handler: MiddlewareHandler) extends Route

enum HandlerResult:
  case Found(f: Future[Response]) extends HandlerResult
  case Next extends HandlerResult
  case Error(error: Any) extends HandlerResult
