package io.github.spritzsn.spritz

import scala.collection.mutable

class Request(
    var method: Method,
    var path: String,
    val headers: mutable.Map[String, String],
    val params: DMap,
    val payload: Array[Byte],
):
  var body: DMap = null
  var route: String = ""
  var rest: String = path

  override def toString: String =
    s"$method $path headers=[${headers.mkString(", ")}] params=[${params.mkString(", ")}] body=$body"
