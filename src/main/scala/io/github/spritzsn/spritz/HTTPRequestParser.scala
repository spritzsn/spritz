package io.github.spritzsn.spritz

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.compiletime.uninitialized

class HTTPRequestParser extends Machine:
  private val MaxMethodLen = 16
  private val MaxUrlLen = 8192
  private val MaxVersionLen = 16
  private val MaxHeaderKeyLen = 256
  private val MaxHeaderValueLen = 8192
  private val MaxHeaderCount = 100
  private val MaxBodySize = 10 * 1024 * 1024 // 10 MB

  val start: State = methodState

  var method: String = null
  val url = new StringBuilder
  var path: String = null
  var version: String = null
  val query = new ListBuffer[(String, String)]
  val headers =
    new mutable.TreeMap[String, String]()(using Ordering.by(_.toLowerCase))
  var key: String = uninitialized
  val buf = new StringBuilder
  val body = new ArrayBuffer[Byte]

  override def reset(): Unit =
    headers.clear()
    buf.clear()
    body.clear()
    query.clear()
    url.clear()
    super.reset()

  def badRequest: Nothing = sys.error("bad request")

  private def urlAcc(c: Int): Unit =
    if url.length >= MaxUrlLen then badRequest
    url += c.toChar

  abstract class AccState(val maxLen: Int) extends State:
    override def enter(): Unit = buf.clear()

    def acc(b: Int): Unit =
      if buf.length >= maxLen then badRequest
      buf += b.toChar

  abstract class NonEmptyAccState(maxLen: Int) extends AccState(maxLen):
    override def exit(): Unit = if buf.isEmpty then badRequest

  case object methodState extends NonEmptyAccState(MaxMethodLen):
    def on = {
      case ' ' =>
        method = buf.toString
        transition(pathState)
      case '\r' | '\n' => badRequest
      case b           => acc(b)
    }

  case object pathState extends NonEmptyAccState(MaxUrlLen):
    def on = {
      case ' ' =>
        path = buf.toString
        transition(versionState)
      case '?' =>
        path = buf.toString
        urlAcc('?')
        transition(queryKeyState)
      case '\r' | '\n' => badRequest
      case b =>
        urlAcc(b)
        acc(b)
    }

  case object queryKeyState extends AccState(MaxHeaderKeyLen):
    val on = {
      case ' ' if buf.nonEmpty => badRequest
      case ' '                 => transition(versionState)
      case '=' if buf.isEmpty  => badRequest
      case '=' =>
        urlAcc('=')
        key = urlDecode(buf.toString)
        transition(queryValueState)
      case '&' => badRequest
      case c =>
        urlAcc(c)
        acc(c)
    }

  case object queryValueState extends AccState(MaxHeaderValueLen):
    override def exit(): Unit =
      query += (key -> urlDecode(buf.toString))

    val on = {
      case ' ' => transition(versionState)
      case '&' =>
        urlAcc('&')
        transition(queryKeyState)
      case '\r' | '=' | '\n' => badRequest
      case c =>
        urlAcc(c)
        acc(c)
    }

  case object versionState extends AccState(MaxVersionLen):
    def on = {
      case '\r' =>
        version = buf.toString
        transition(value2keyState)
      case '\n' => badRequest
      case b    => acc(b)
    }

  case object headerValueState extends AccState(MaxHeaderValueLen):
    def on = {
      case '\r' =>
        headers(key) = buf.toString
        transition(value2keyState)
      case '\n' => badRequest
      case b    => acc(b)
    }

  case object value2keyState extends State:
    def on = {
      case '\n' => transition(headerKeyState)
      case _    => badRequest
    }

  case object headerKeyState extends NonEmptyAccState(MaxHeaderKeyLen):
    def on = {
      case '\r' if buf.nonEmpty => badRequest
      case '\r'                 => directTransition(blankState)
      case ':' =>
        if headers.size >= MaxHeaderCount then badRequest
        key = buf.toString
        transition(key2valueState)
      case '\n' => badRequest
      case b    => acc(b)
    }

  case object blankState extends State:
    def on = {
      case '\n' if headers contains "Content-Length" => transition(bodyState)
      case '\n'                                      => transition(FINAL)
      case _                                         => badRequest
    }

  case object bodyState extends State:
    var len: Int = 0

    override def enter(): Unit =
      val raw = headers("Content-Length").trim
      if raw.isEmpty || !raw.forall(_.isDigit) then badRequest

      val parsed =
        try raw.toLong
        catch case _: NumberFormatException => badRequest
      if parsed < 0 || parsed > MaxBodySize then badRequest

      len = parsed.toInt
      if len == 0 then transition(FINAL)

    def on = { case b =>
      body += b.toByte

      if body.length == len then transition(FINAL)
    }

  case object key2valueState extends State:
    def on = {
      case ' '  =>
      case '\n' => badRequest
      case '\r' =>
        headers(key) = ""
        transition(value2keyState)
      case v =>
        pushback(v)
        transition(headerValueState)
    }

  override def toString: String =
    s"${super.toString}, request line: [$method $url $version], headers: $headers, body: $body, length: ${body.length}"
end HTTPRequestParser
