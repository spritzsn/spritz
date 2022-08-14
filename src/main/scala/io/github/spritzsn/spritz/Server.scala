package io.github.spritzsn.spritz

import scala.collection.mutable.ArrayBuffer
import scala.collection.{immutable, mutable}
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import io.github.spritzsn.libuv._

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

    def readCallback(client: TCP, size: Int, buf: Buffer): Unit =
//      client.write(
//        s"""HTTP/1.0 200 OK\r
//           |Content-Type: text/plain\r
//           |Content-Length: 12\r
//           |\r
//           |hello world
//           |""".stripMargin.getBytes,
//      )
      client.readStop
      client.shutdown(_.close(_ => ()))

    client.readStart(readCallback)

    server accept client
  end connectionCallback

  def listen(port: Int, serverName: String = null, flags: Int = 0, backlog: Int = 4096): Unit =
    if serverName ne null then _serverName = Some(serverName)

    val server = loop.tcp

    server.bind("0.0.0.0", port, flags)
    server.listen(backlog, connectionCallback)

  protected class Connection:
    val parser = new RequestParser

  protected val connectionMap = new mutable.HashMap[TCPHandle, Connection]

  val ALLOC_SIZE = 1024

  val allocateCB: AllocCB =
    (client: TCPHandle, size: CSize, buffer: Ptr[Buffer]) =>
      buffer._1 = malloc(ALLOC_SIZE.toUInt)
      buffer._2 = ALLOC_SIZE.toUInt

  def shutdown(client: TCPHandle): Unit = {
    val shutdown_req = malloc(uv_req_size(UV_SHUTDOWN_REQ_T))
      .asInstanceOf[ShutdownReq]
    !shutdown_req = client.asInstanceOf[Ptr[Byte]]
    checkError(uv_shutdown(shutdown_req, client, shutdownCB), "uv_shutdown")
  }

  val shutdownCB: ShutdownCB =
    (shutdownReq: ShutdownReq, status: Int) =>
      val client = (!shutdownReq).asInstanceOf[TCPHandle]

      uv_close(client, closeCB)
      free(shutdownReq.asInstanceOf[Ptr[Byte]])
  end shutdownCB

  val readCB: ReadCB =
    (client: TCPHandle, size: CSSize, buffer: Ptr[Buffer]) =>
      if (size < 0) shutdown(client)
      else
        val conn = connectionMap(client)

        try for i <- 0 until size.toInt do conn.parser send !(buffer._1 + i)
        catch
          case e: Exception =>
            respond(new Response(_serverName).sendStatus(400), client)
            shutdown(client)

        free(buffer._1)

        if conn.parser.isDone then
          try
            process(conn.parser, client).foreach { res =>
              respond(res, client)
              shutdown(client)
            }
          catch
            case e: Exception =>
              respond(new Response(_serverName).sendStatus(500), client)
              shutdown(client)
  end readCB

  val closeCB: CloseCB =
    (client: TCPHandle) =>
      free(client.asInstanceOf[Ptr[Byte]])
      connectionMap -= client
      ()
  end closeCB

  val onConnectionCB: ConnectionCB =
    (handle: TCPHandle, status: Int) =>
      val client = malloc(uv_handle_size(UV_TCP_T)).asInstanceOf[TCPHandle]

      checkError(uv_tcp_init(loop, client), "uv_tcp_init(client)")
      checkError(uv_accept(handle, client), "uv_accept")
      checkError(uv_read_start(client, allocateCB, readCB), "uv_read_start")
      connectionMap(client) = new Connection
  end onConnectionCB

  def process(httpreq: RequestParser, client: TCPHandle): Future[Response] =
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
      case HandlerResult.Next       => sys.error("HandlerResult.Next")
      case HandlerResult.Error(err) => sys.error(s"HandlerResult.Error($err)")
  end process

  val writeCB: WriteCB =
    (writeReq: WriteReq, status: Int) =>
      val buffer = (!writeReq).asInstanceOf[Ptr[Buffer]]

      free(buffer._1)
      free(buffer.asInstanceOf[Ptr[Byte]])
      free(writeReq.asInstanceOf[Ptr[Byte]])

  def respond(res: Response, client: TCPHandle): Unit =
    val writeReq = malloc(uv_req_size(UV_WRITE_REQ_T)).asInstanceOf[WriteReq]
    val buffer = malloc(sizeof[Buffer]).asInstanceOf[Ptr[Buffer]]
    val array = res.responseArray
    val data = malloc(array.length.toUInt)

    for i <- array.indices do data(i) = array(i)

    buffer._1 = data
    buffer._2 = array.length.toUInt
    !writeReq = buffer.asInstanceOf[Ptr[Byte]]
    checkError(uv_write(writeReq, client, buffer, 1, writeCB), "uv_write")
