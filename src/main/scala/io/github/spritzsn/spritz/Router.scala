package io.github.spritzsn.spritz

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.util.matching.Regex
import io.github.spritzsn.async._

class Router extends RequestHandler2:

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

  protected def endpoint(method: Method, path: String, handler: RequestHandler): Router =
    val (pathr, params) = regex(path)

    routes += Route.Endpoint(method, pathr, params, handler)
    this

  def get(path: String, handler: RequestHandler): Router = endpoint("GET", path, handler)

  def post(path: String, handler: RequestHandler): Router = endpoint("POST", path, handler)

  def put(path: String, handler: RequestHandler): Router = endpoint("PUT", path, handler)

  def delete(path: String, handler: RequestHandler): Router = endpoint("DELETE", path, handler)

  def patch(path: String, handler: RequestHandler): Router = endpoint("PATCH", path, handler)

  def use(path: String, middleware: RequestHandler): Router =
    val (pathr, params) = regex(path)

    routes += Route.Routes(pathr, params, middleware)
    this

  def use(middleware: RequestHandler): Router =
    routes += Route.Middleware(middleware)
    this

  protected def routeMatch(req: Request, params: Seq[String], m: Regex.Match): Unit =
    params foreach (k => req.params(k) = Util.urlDecode(m.group(k)))
    req.route ++= req.rest.substring(0, m.end)
    req.rest = req.rest.substring(m.end)

  def apply(req: Request, res: Response): HandlerResult =
    for route <- routes do
      var next = false
      var error: Option[Any] = None

      def callHandler(handler: RequestHandler): HandlerResult =
        val result =
          handler match
            case handler2: ((_, _) => _) => handler2.asInstanceOf[RequestHandler2](req, res)
            case handler3: ((_, _, _) => _) =>
              def nextFunction(err: Any): Unit =
                if err != () then error = Some(err)
                next = true

              handler3.asInstanceOf[RequestHandler3](req, res, nextFunction)
            case handler4: ((_, _, _, _) => _) => sys.error("error handlers are not supported yet")

        result match
          case r: HandlerResult      => r
          case f: Future[_] if !next => HandlerResult.Found(f map (_ => res))
          case _ if !next            => HandlerResult.Found(Future(res))
          case _ if error.isEmpty    => HandlerResult.Next
          case _                     => HandlerResult.Error(error.get)
      end callHandler

      route match
        case Route.Endpoint(method, path, params, handler) =>
          if method == req.method || req.method == "HEAD" && method == "GET" then
            path.findPrefixMatchOf(req.rest) match
              case Some(m) if m.end == req.rest.length =>
                routeMatch(req, params, m)

                callHandler(handler) match
                  case HandlerResult.Found(f) =>
                    return HandlerResult.Found(f andThen (_ => if req.method == "HEAD" then res.body = Array()))
                  case HandlerResult.Next     =>
                  case e: HandlerResult.Error => return e
              case _ =>
        case Route.Routes(path, params, handler) =>
          path.findPrefixMatchOf(req.rest) match
            case Some(m) =>
              routeMatch(req, params, m)
              callHandler(handler) match
                case f: HandlerResult.Found => return f
                case HandlerResult.Next     =>
                case e: HandlerResult.Error => return e
            case _ =>
        case Route.Middleware(handler) =>
          callHandler(handler) match
            case f: HandlerResult.Found => return f
            case HandlerResult.Next     =>
            case e: HandlerResult.Error => return e
    end for

    HandlerResult.Next
