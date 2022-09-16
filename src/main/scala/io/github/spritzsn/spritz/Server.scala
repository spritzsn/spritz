package io.github.spritzsn.spritz

import scala.collection.mutable.ArrayBuffer
import scala.collection.{immutable, mutable}
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import io.github.spritzsn.libuv._
import io.github.spritzsn.async

object Server extends Router:
  var exceptionHandler: (Response, Throwable) => Unit =
    (res, ex) => res.status(500).send(s"exception '${ex.getClass}': ${ex.getMessage}")

  def apply(serverName: String = null)(routing: Server.type => Unit): Unit =
    if serverName ne null then
      use { (_: Request, res: Response) =>
        res.setIfNot("Server", serverName)
        HandlerResult.Next
      }
    routing(this)
    use { (req: Request, res: Response) =>
      res
        .status(404)
        .send(s"""
           |<!DOCTYPE html>
           |<html>
           |  <head>
           |    <title>404 Not Found</title>
           |  </head>
           |  <body>
           |    <h1>404 Not Found</h1>
           |    <p>no matching routes for path '<code>${req.originalPath}</code>'</p>
           |  </body>
           |</html>
           |""".stripMargin)
    }
    async.loop.run()

  private def connectionCallback(server: TCP, status: Int): Unit =
    if status < 0 then Console.err.println(s"connection error: ${strError(status)}")
    else
      val client = defaultLoop.tcp
      val parser = new HTTPRequestParser

      def readCallback(client: TCP, size: Int, buf: Buffer): Unit =
        if size < 0 then
          client.readStop

          if size != eof then Console.err.println(s"error in read callback: ${errName(size)}: ${strError(size)}")

          close(client)
        else if size > 0 then
          try
            var i = 0

            while i < size do
              parser send buf(i)
              i += 1

            if parser.isFinal then
              process(parser, client) onComplete {
                case Success(res) =>
                  try respond(res, client, false)
                  catch case _: Exception => close(client)
                case Failure(e) =>
                  val res = new Response()

                  exceptionHandler(res, e)
                  respond(res, client, true)
              }
              parser.reset()
          catch case e: Exception => respond(new Response().status(400).send(e.getMessage), client, true)
      end readCallback

      server accept client // according to docs (http://docs.libuv.org/en/v1.x/stream.html#c.uv_accept), this is guaranteed not to fail
      client readStart readCallback // todo: http://docs.libuv.org/en/v1.x/stream.html#c.uv_read_start
    end if
  end connectionCallback

  def close(client: TCP): Unit = if client.isClosing then client.dispose() else client.close()

  def respond(res: Response, client: TCP, closeSocket: Boolean): Unit =
    if closeSocket then client.readStop

    if client.isWritable then
      client.write(res.responseArray)
      if closeSocket then client.shutdown(_.close())
    else if closeSocket then close(client)

  def listen(port: Int, flags: Int = 0, backlog: Int = 4096): Unit =
    val server = defaultLoop.tcp

    server.bind("0.0.0.0", port, flags)
    server.listen(backlog, connectionCallback)

  def process(httpreq: HTTPRequestParser, client: TCP): Future[Response] =
    val req =
      new Request(
        httpreq.method.asInstanceOf[Method],
        httpreq.path,
        httpreq.url.toString,
        httpreq.query,
        httpreq.version,
        httpreq.headers,
        new DMap,
        httpreq.body.toArray,
        client.getPeerName,
        httpreq.headers.getOrElse("Host", ""),
      )
    val res = new Response(headOnly = req.method == "HEAD")

    apply(req, res) map {
      case HandlerResult.Responded  => res
      case HandlerResult.Next       => sys.error("HandlerResult.Next")
      case HandlerResult.Error(err) => sys.error(s"HandlerResult.Error($err)") // todo
    }
  end process
