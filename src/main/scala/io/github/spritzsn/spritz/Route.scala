package io.github.spritzsn.spritz

import scala.concurrent.Future
import scala.util.matching.Regex

type NextFunction = Any => Unit

type HandlerReturnType = HandlerResult | Future[_] | Response | Unit
type RequestHandler2 = (Request, Response) => HandlerReturnType
type RequestHandler3 = (Request, Response, NextFunction) => HandlerReturnType
type RequestHandler4 = (Request, Response, NextFunction, Any) => HandlerReturnType
type RequestHandler = RequestHandler2 | RequestHandler3 | RequestHandler4

type Method = "HEAD" | "GET" | "POST" | "PUT" | "DELETE" | "PATCH" | "OPTIONS"

enum Route:
  case Endpoint(method: Method, path: Regex, params: Seq[String], handlers: Seq[RequestHandler]) extends Route
  case Routes(path: Regex, params: Seq[String], handler: RequestHandler) extends Route
  case Middleware(handler: RequestHandler) extends Route

enum HandlerResult:
  case Found(f: Future[Response]) extends HandlerResult
  case Next extends HandlerResult
  case Error(error: Any) extends HandlerResult
