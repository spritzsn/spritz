package io.github.spritzsn.spritz

import java.time.format.DateTimeFormatter
import java.time.{ZoneId, ZonedDateTime}
import scala.collection.{immutable, mutable}
import scala.collection.mutable.ArrayBuffer
import scala.io.Codec

class Response(serverName: Option[String], zoneId: ZoneId = ZoneId.of("GMT")):
  var statusCode: Option[Int] = None
  var statusMessage: String = "None"
  val headers = new mutable.LinkedHashMap[String, String]
  var body: Array[Byte] = Array()
  val locals = new mutable.HashMap[String, Any]

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
    setIfNot("Content-Type") { "application/octet-stream" }
    body = data
    statusIfNone(200)
    headers("Content-Length") = body.length.toString
    this

  def send(obj: Any): Response =
    val s = String.valueOf(obj)

    setIfNot("Content-Type") {
      if s startsWith "<" then "text/html; charset=UTF-8" else "text/plain; charset=UTF-8"
    }
    send(Codec.toUTF8(s))

  def setIfNot(key: String)(value: => String): Response =
    if !(headers contains key) then headers(key) = value
    this

  def responseArray: ArrayBuffer[Byte] =
    serverName foreach (n => setIfNot("Server") { n })
    setIfNot("Date") { DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(zoneId)) }

    val buf = new ArrayBuffer[Byte]

    def eol = buf ++= "\r\n".getBytes

    buf ++= s"HTTP/1.0 ${statusCode.getOrElse(500)} $statusMessage".getBytes
    eol

    for (k, v) <- headers do
      buf ++= s"$k: $v".getBytes
      eol

    eol
    buf ++= body
    buf

  override def toString: String =
    s"--- HTTP Response Begin ---\n${Codec.fromUTF8(responseArray.toArray).mkString}\n--- HTTP Response End ---"
