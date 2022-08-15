package io.github.spritzsn.spritz

import scala.collection.mutable.ArrayBuffer
import scala.collection.{immutable, mutable}
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import io.github.spritzsn.libuv._
import io.github.spritzsn.async._

object Server extends Router:
  val loop: Loop = defaultLoop

  var _serverName: Option[String] = None

  def apply(routing: Server.type => Unit): Unit =
    routing(this)
    use { (req, res) =>
      res.status(404).send(s"no matching routes for path '${req.path}'")
      HandlerResult.Found(Future { () })
    }

  def connectionCallback(server: TCP, status: Int): Unit =
    val client = defaultLoop.tcp
    val parser = new RequestParser

    def readCallback(client: TCP, size: Int, buf: Buffer): Unit =
      def end(): Unit =
        client.readStop
        client.shutdown(_.close(_ => ()))

      if size < 0 then end()
      else
        Try(for i <- 0 until size do parser send buf(i)) match
          case Failure(exception) =>
            respond(new Response(_serverName).status(400).send(exception.getMessage), client)
            end()
          case Success(_) =>
            if parser.isDone then
              try
                process(parser, client) foreach { res =>
                  respond(res, client)
                  end()
                }
              catch
                case e: Exception =>
                  respond(new Response(_serverName).sendStatus(500), client)
                  end()

      buf.dispose()
    end readCallback

    client.readStart(readCallback)
    server accept client
  end connectionCallback

  def respond(res: Response, client: TCP): Unit = client.write(res.responseArray)

  def listen(port: Int, serverName: String = null, flags: Int = 0, backlog: Int = 4096): Unit =
    if serverName ne null then _serverName = Some(serverName)

    val server = loop.tcp

    server.bind("0.0.0.0", port, flags)
    server.listen(backlog, connectionCallback)

  def process(httpreq: RequestParser, client: TCP): Future[Response] =
    val res = new Response(_serverName)
    val req =
      new Request(
        httpreq.requestLine.head.asInstanceOf[Method],
        httpreq.requestLine(1),
        httpreq.headers,
        new DMap,
        httpreq.body.toArray,
      )

    apply(req, res) match
      case HandlerResult.Found(f)   => f.map(_ => res)
      case HandlerResult.Next       => sys.error("HandlerResult.Next") // todo
      case HandlerResult.Error(err) => sys.error(s"HandlerResult.Error($err)") // todo
  end process
