package io.github.spritzsn.spritz

import scala.concurrent.Future
import scala.util.matching.Regex

type AsyncEndpointHandler = (Request, Response) => Future[_]

type EndpointHandler = (Request, Response) => Unit

type MiddlewareHandler = (Request, Response) => HandlerResult

type Method = "GET" | "POST" | "PUT" | "DELETE" | "PATCH"

enum Route:
  case EndpointAsync(method: Method, path: Regex, params: Seq[String], handler: AsyncEndpointHandler) extends Route
  case PathRoutes(path: Regex, params: Seq[String], handler: MiddlewareHandler) extends Route
  case Middleware(handler: MiddlewareHandler) extends Route

enum HandlerResult:
  case Found(future: Future[_]) extends HandlerResult
  case Next extends HandlerResult
  case Error(err: Any) extends HandlerResult
