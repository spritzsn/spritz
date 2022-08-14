package io.github.spritzsn.spritz

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, ListBuffer}

class RequestParser extends Machine:
  val start: State = methodState

  val requestLine = new ListBuffer[String]
  var headers =
    new mutable.TreeMap[String, String]()(scala.math.Ordering.comparatorToOrdering(String.CASE_INSENSITIVE_ORDER))
  var key: String = _
  val buf = new StringBuilder
  val body = new ArrayBuffer[Byte]

  def acc(b: Int): Unit = buf += b.toByte.toChar

  def br: Nothing = sys.error("bad request")

  abstract class AccState extends State:
    override def enter(): Unit = buf.clear()

    override def exit(): Unit = if buf.isEmpty then br

  class RequestLineState(next: State) extends AccState:
    def on = {
      case ' ' =>
        requestLine += buf.toString
        transition(next)
      case '\r' | '\n' => br
      case b           => acc(b)
    }

  case object versionState extends AccState:
    def on = {
      case '\r' =>
        requestLine += buf.toString
        transition(value2keyState)
      case '\n' => br
      case b    => acc(b)
    }

  case object valueState extends AccState:
    def on = {
      case '\r' =>
        headers(key) = buf.toString
        transition(value2keyState)
      case '\n' => br
      case b    => acc(b)
    }

  case object value2keyState extends State:
    def on = {
      case '\n' => transition(keyState)
      case _    => br
    }

  case object keyState extends AccState:
    def on = {
      case '\r' if buf.nonEmpty => br
      case '\r'                 => directTransition(blankState)
      case ':' =>
        key = buf.toString
        transition(key2valueState)
      case '\n' => br
      case b    => acc(b)
    }

  case object blankState extends State:
    def on = {
      case '\n' =>
        transition(if headers contains "Content-Length" then bodyState else DONE)
      case _ => br
    }

  case object bodyState extends State:
    var len: Int = 0

    override def enter(): Unit =
      len = headers("Content-Length").toInt

      if len == 0 then transition(DONE)

    def on = { case b =>
      body += b.toByte

      if body.length == len then transition(DONE)
    }

  case object key2valueState extends State:
    def on = {
      case ' '         =>
      case '\r' | '\n' => br
      case v =>
        pushback(v)
        transition(valueState)
    }

  case object methodState extends RequestLineState(pathState)
  case object pathState extends RequestLineState(versionState)

  override def toString: String =
    s"${super.toString}, request line: [$requestLine], headers: $headers, body: $body, length: ${body.length}"
end RequestParser
