package io.github.spritzsn.spritz

import scala.collection.mutable.ArrayBuffer
import scala.collection.{immutable, mutable}
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import io.github.spritzsn.libuv._
import io.github.spritzsn.async

object Server extends Router:
  val loop: Loop = defaultLoop

  def apply(serverName: String = null)(routing: Server.type => Unit): Unit =
    if serverName ne null then
      use { (_: Request, res: Response) =>
        res.setIfNot("Server")(serverName)
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
           |    <p>no matching routes for path '<code>${req.path}</code>'</p>
           |  </body>
           |</html>
           |""".stripMargin)
    }
    async.loop.run()

  def connectionCallback(server: TCP, status: Int): Unit =
    val client = defaultLoop.tcp
    val parser = new RequestParser

    def readCallback(client: TCP, size: Int, buf: Buffer): Unit =
      if size < 0 then
        client.readStop
        if size != eof then println(s"error in read callback: ${errName(size)}: ${strError(size)}") // todo
      else if size > 0 then
        try
          for i <- 0 until size do parser send buf(i)

          if parser.isFinal then
            process(parser) onComplete {
              case Success(res) =>
                try respond(res, client)
                catch case e: Exception => respond(new Response().status(500).send(e.getMessage), client)
              case Failure(e) => respond(new Response().status(500).send(e.getMessage), client)
            }
        catch case e: Exception => respond(new Response().status(400).send(e.getMessage), client)
    end readCallback

    server accept client
    client readStart readCallback
  end connectionCallback

  def respond(res: Response, client: TCP): Unit =
    client.readStop

    if client.isWritable then
      client.write(res.responseArray)
      client.shutdown(_.close())
    else if client.isClosing then client.dispose()
    else client.close()

  def listen(port: Int, flags: Int = 0, backlog: Int = 4096): Unit =
    val server = loop.tcp

    server.bind("0.0.0.0", port, flags)
    server.listen(backlog, connectionCallback)

  def process(httpreq: RequestParser): Future[Response] =
    val res = new Response()
    val req =
      new Request(
        httpreq.requestLine.head.asInstanceOf[Method],
        httpreq.requestLine(1),
        httpreq.headers,
        new DMap,
        httpreq.body.toArray,
      )

    Future(apply(req, res)) flatMap {
      case HandlerResult.Found(f)   => f
      case HandlerResult.Next       => sys.error("HandlerResult.Next")
      case HandlerResult.Error(err) => sys.error(s"HandlerResult.Error($err)") // todo
    }
  end process
