package io.github.spritzsn.spritz

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.util.matching.Regex
import io.github.spritzsn.async._

class Router extends MiddlewareHandler:

  private[spritz] val routes = new ListBuffer[Route]

  private def regex(path: String): (Regex, Seq[String]) =
    val buf = new mutable.StringBuilder
    val groups = new ListBuffer[String]

    def regex(elem: RouteAST): Unit =
      elem match
        case RouteAST.Slash            => buf ++= "/"
        case RouteAST.Literal(segment) => buf ++= segment
        case RouteAST.Parameter(name) =>
          buf ++= s"(?<$name>[^/#\\?]+)"
          groups += name
        case RouteAST.Sequence(elems) => elems foreach regex

    buf += '^'

    RouteParser(path) match
      case RouteAST.Slash => buf ++= "/?"
      case ast            => regex(ast)

    (buf.toString.r, groups.toSeq)

  protected def endpoint(method: Method, path: String, handler: EndpointHandler): Router =
    val (pathr, params) = regex(path)

    routes += Route.Endpoint(method, pathr, params, handler)
    this

  def get(path: String, handler: EndpointHandler): Router = endpoint("GET", path, handler)

  def post(path: String, handler: EndpointHandler): Router = endpoint("POST", path, handler)

  def put(path: String, handler: EndpointHandler): Router = endpoint("PUT", path, handler)

  def delete(path: String, handler: EndpointHandler): Router = endpoint("DELETE", path, handler)

  def patch(path: String, handler: EndpointHandler): Router = endpoint("PATCH", path, handler)

  def use(path: String, middleware: MiddlewareHandler): Router =
    val (pathr, params) = regex(path)

    routes += Route.PathRoutes(pathr, params, middleware)
    this

  def use(middleware: MiddlewareHandler): Router =
    routes += Route.Middleware(middleware)
    this

  protected def routeMatch(req: Request, params: Seq[String], m: Regex.Match): Unit =
    params foreach (k => req.params(k) = Util.urlDecode(m.group(k)))
    req.route ++= req.rest.substring(0, m.end)
    req.rest = req.rest.substring(m.end)

  def apply(req: Request, res: Response): HandlerResult =
    for route <- routes do
      route match
        case Route.Endpoint(method, path, params, handler) =>
          if method == req.method || req.method == "HEAD" && method == "GET" then
            path.findPrefixMatchOf(req.rest) match
              case Some(m) if m.end == req.rest.length =>
                routeMatch(req, params, m)

                val ret =
                  handler(req, res) match
                    case f: Future[_] => f map (_ => res)
                    case _            => Future(res)

                return HandlerResult.Found(ret andThen (_ => if req.method == "HEAD" then res.body = Array()))
              case _ =>
        case Route.PathRoutes(path, params, handler) =>
          path.findPrefixMatchOf(req.rest) match
            case Some(m) =>
              routeMatch(req, params, m)
              handler(req, res) match
                case f: HandlerResult.Found   => return f
                case HandlerResult.Next       =>
                case HandlerResult.Error(err) => return HandlerResult.Error(err)
            case _ =>
        case Route.Middleware(handler) =>
          handler(req, res) match
            case f: HandlerResult.Found   => return f
            case HandlerResult.Next       =>
            case HandlerResult.Error(err) => return HandlerResult.Error(err)
    end for

    HandlerResult.Next
