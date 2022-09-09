package io.github.spritzsn.spritz

import java.time.format.DateTimeFormatter
import java.time.{ZoneId, ZonedDateTime}
import scala.collection.{immutable, mutable}
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.io.Codec

import io.github.edadma.json.DefaultJSONWriter

class Response(headOnly: Boolean = false, val zoneId: ZoneId = ZoneId.of("GMT")):
  var statusCode: Option[Int] = None
  var statusMessage: String = "None"
  val headers =
    new mutable.TreeMap[String, String]()(scala.math.Ordering.comparatorToOrdering(String.CASE_INSENSITIVE_ORDER))
  val headersList = new ListBuffer[(String, String)]
  var body: Array[Byte] = Array()
  val locals = new DMap
  private val actions = new ArrayBuffer[() => Unit]

  def action(thunk: => Unit): Unit = actions += (() => thunk)

  def status(code: Int): Response =
    statusCode = Some(code)
    statusMessage = HTTP.statusMessageString(code)
    this

  def statusIfNone(code: Int): Response =
    if statusCode.isEmpty then status(code)
    this

  def sendStatus(code: Int): Response =
    status(code).send(s"<h1>${HTTP.statusMessageString(code)}</h1>")
    this

  def send(data: Array[Byte]): Response =
    typ("application/octet-stream")
    body = data
    statusIfNone(200)
    set("Content-Length", body.length)
    this

  def typ(s: String): Response = setIfNot("Content-Type", if s contains "/" then s else contentType(s))

  def json(data: Any, tab: Int = 0): Response =
    typ("application/json; charset=UTF-8")
    body = stringify(data, tab, tab > 0).getBytes
    statusIfNone(200)
    set("Content-Length", body.length)
    this

  def send(obj: Any): Response =
    val s = String.valueOf(obj)

    typ(
      if s.trim startsWith "<" then "text/html; charset=UTF-8" // todo: should be s.stripLeading not s.trim
      else "text/plain; charset=UTF-8",
    )
    send(Codec.toUTF8(s))

  def set(key: String, value: Any): Response =
    val v = String.valueOf(value)

    if headers contains key then
      headersList.indexWhere { case (k, _) => k equalsIgnoreCase key } match
        case -1  =>
        case idx => headersList remove idx

    headersList += key -> v
    headers(key) = v
    this

  def setIfNot(key: String, value: => Any): Response =
    if !(headers contains key) then set(key, value)
    this

  def responseArray: ArrayBuffer[Byte] =
    setIfNot("Date", DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(zoneId)))

    var i = 0

    while i < actions.length do
      try actions(i)()
      catch
        case e: Exception =>
          val res = new Response(false, zoneId)

          res.status(500).send(s"exception in middleware action: ${e.getMessage}")
          return res.responseArray

      i += 1

    val buf = new ArrayBuffer[Byte]

    def eol = buf ++= "\r\n".getBytes

    buf ++= s"HTTP/1.0 ${statusCode.getOrElse(500)} $statusMessage".getBytes
    eol

    for (k, v) <- headersList do
      buf ++= s"$k: $v".getBytes
      eol

    eol
    if !headOnly then buf ++= body
    buf

  override def toString: String =
    s"--- HTTP Response Begin ---\n${Codec.fromUTF8(responseArray.toArray).mkString}\n--- HTTP Response End ---"
