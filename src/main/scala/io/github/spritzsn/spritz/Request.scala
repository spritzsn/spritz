package io.github.spritzsn.spritz

import scala.collection.mutable

class Request(
    val method: Method,
    val originalPath: String,
    val originalUrl: String,
    val query: DMap,
    val version: String,
    val headers: mutable.Map[String, String],
    val params: DMap,
    val payload: Array[Byte],
    val ip: String,
    val hostname: String,
):
  var body: DMap = null
  var route: String = ""
  var rest: String = originalPath

  def get(header: String): Option[String] = headers get header

  override def toString: String =
    s"$method $originalUrl headers=[${headers.mkString(", ")}] params=[${params.mkString(", ")}] body=$body"
