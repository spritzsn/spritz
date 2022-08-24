package io.github.spritzsn.spritz

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, ListBuffer}

class HTTPRequestParser extends Machine:
  val start: State = methodState

  var method: String = null
  val url = new StringBuilder
  var path: String = null
  var version: String = null
  val query = new DMap
  val headers =
    new mutable.TreeMap[String, String]()(scala.math.Ordering.comparatorToOrdering(String.CASE_INSENSITIVE_ORDER))
  var key: String = _
  val buf = new StringBuilder
  val body = new ArrayBuffer[Byte]

  def badRequest: Nothing = sys.error("bad request")

  abstract class AccState extends State:
    override def enter(): Unit = buf.clear()

    def acc(b: Int): Unit = buf += b.toChar

  abstract class NonEmptyAccState extends AccState:
    override def exit(): Unit = if buf.isEmpty then badRequest

  case object methodState extends NonEmptyAccState:
    def on = {
      case ' ' =>
        method = buf.toString
        transition(pathState)
      case '\r' | '\n' => badRequest
      case b           => acc(b)
    }

  case object pathState extends NonEmptyAccState:
    def on = {
      case ' ' =>
        path = buf.toString
        transition(versionState)
      case '?' =>
        path = buf.toString
        url += '?'
        transition(queryKeyState)
      case '\r' | '\n' => badRequest
      case b =>
        url += b.toChar
        acc(b)
    }

  case object queryKeyState extends AccState:
    val on = {
      case ' ' if buf.nonEmpty => badRequest
      case ' '                 => transition(versionState)
      case '=' if buf.isEmpty  => badRequest
      case '=' =>
        url += '='
        key = urlDecode(buf.toString)
        transition(queryValueState)
      case '&' => badRequest
      case c =>
        url += c.toChar
        acc(c)
    }

  case object queryValueState extends AccState:
    override def exit(): Unit = query(key) = urlDecode(buf.toString)

    val on = {
      case ' ' => transition(versionState)
      case '&' =>
        url += '&'
        transition(headerKeyState)
      case '\r' | '=' | '\n' => badRequest
      case c =>
        url += '='
        acc(c)
    }

  case object versionState extends AccState:
    def on = {
      case '\r' =>
        version = buf.toString
        transition(value2keyState)
      case '\n' => badRequest
      case b    => acc(b)
    }

  case object headerValueState extends AccState:
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

  case object headerKeyState extends NonEmptyAccState:
    def on = {
      case '\r' if buf.nonEmpty => badRequest
      case '\r'                 => directTransition(blankState)
      case ':' =>
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
      len = headers("Content-Length").toInt

      if len == 0 then transition(FINAL)

    def on = { case b =>
      body += b.toByte

      if body.length == len then transition(FINAL)
    }

  case object key2valueState extends State:
    def on = {
      case ' '         =>
      case '\r' | '\n' => badRequest
      case v =>
        pushback(v)
        transition(headerValueState)
    }

  override def toString: String =
    s"${super.toString}, request line: [$method $url $version], headers: $headers, body: $body, length: ${body.length}"
end HTTPRequestParser
