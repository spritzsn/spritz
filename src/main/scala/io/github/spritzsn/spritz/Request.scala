package io.github.spritzsn.spritz

import scala.collection.mutable

class Request(
    val method: Method,
    val originalUrl: String,
    val headers: mutable.Map[String, String],
    val params: DMap,
    val payload: Array[Byte],
):
  val (originalPath, queryString) =
    originalUrl indexOf '?' match
      case -1  => (originalUrl, "")
      case idx => (originalUrl.substring(0, idx), originalUrl.substring(idx + 1))
  var body: DMap = null
  var route: String = ""
  var rest: String = originalPath

  override def toString: String =
    s"$method $originalUrl headers=[${headers.mkString(", ")}] params=[${params.mkString(", ")}] body=$body"
