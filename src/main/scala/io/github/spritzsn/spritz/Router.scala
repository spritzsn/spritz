package io.github.spritzsn.spritz

import scala.annotation.tailrec
import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.concurrent.Future
import scala.util.matching.Regex

class Router extends RequestHandler:
  private[spritz] val routes = new ArrayBuffer[Route]

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

  protected def endpoint(method: Method, path: String, handler: RequestHandler, handlers: Seq[RequestHandler]): Router =
    val (pathr, params) = regex(path)

    routes += Route.Endpoint(method, pathr, params, handler)
    for h <- handlers do routes += Route.Same(h)
    this

  def get(path: String, handler: RequestHandler, handlers: RequestHandler*): Router =
    endpoint("GET", path, handler, handlers)

  def post(path: String, handler: RequestHandler, handlers: RequestHandler*): Router =
    endpoint("POST", path, handler, handlers)

  def put(path: String, handler: RequestHandler, handlers: RequestHandler*): Router =
    endpoint("PUT", path, handler, handlers)

  def delete(path: String, handler: RequestHandler, handlers: RequestHandler*): Router =
    endpoint("DELETE", path, handler, handlers)

  def patch(path: String, handler: RequestHandler, handlers: RequestHandler*): Router =
    endpoint("PATCH", path, handler, handlers)

  def use(path: String, middleware: RequestHandler): Router =
    val (pathr, params) = regex(path)

    routes += Route.Path(pathr, params, middleware)
    this

  def use(middleware: RequestHandler): Router =
    routes += Route.Middleware(middleware)
    this

  protected def routeMatch(req: Request, params: Seq[String], m: Regex.Match): Unit =
    params foreach (k => req.params(k) = urlDecode(m.group(k)))
    req.route ++= req.rest.substring(0, m.end)
    req.rest = req.rest.substring(m.end)

  def apply(req: Request, res: Response): Future[HandlerResult] =
    def find(from: Int): Option[(Int, RequestHandler)] =
      var i = from
      var matched = true

      while i < routes.length do
        routes(i) match
          case Route.Endpoint(method, path, params, handler) =>
            if method == req.method || req.method == "HEAD" && method == "GET" then
              path.findPrefixMatchOf(req.rest) match
                case Some(m) if m.end == req.rest.length =>
                  routeMatch(req, params, m)
                  return Some((i + 1, handler))
                case _ =>
          case Route.Same(handler) if matched => return Some((i + 1, handler))
          case Route.Same(_)                  =>
          case Route.Middleware(handler)      => return Some((i + 1, handler))
          case Route.Path(path, params, handler) =>
            path.findPrefixMatchOf(req.rest) match
              case Some(m) =>
                routeMatch(req, params, m)
                return Some((i + 1, handler))
              case _ =>

        matched = false
        i += 1

      None
    end find

    def run(from: Int): Future[HandlerResult] =
      find(from) match
        case Some((idx, handler)) =>
          handler(req, res) match
            case HandlerResult.Next     => run(idx)
            case e: HandlerResult.Error => Future(e)
            case f: Future[_] =>
              f flatMap {
                _ match
                  case HandlerResult.Next     => run(idx)
                  case e: HandlerResult.Error => Future(e)
                  case _                      => Future(HandlerResult.Responded)
              }
            case _ => Future(HandlerResult.Responded)
        case None => Future(HandlerResult.Next)

    run(0)
