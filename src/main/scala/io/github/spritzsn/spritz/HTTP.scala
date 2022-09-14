package io.github.spritzsn.spritz

object HTTP:
  val statusMessage: Map[Int, String] =
    Map(
      100 -> "Continue",
      101 -> "Switching Protocols",
      102 -> "Processing",
      103 -> "Early Hints",
      200 -> "OK",
      201 -> "Created",
      202 -> "Accepted",
      203 -> "Non-Authoritative Information",
      204 -> "No Content",
      301 -> "Moved Permanently",
      304 -> "Not Modified",
      400 -> "Bad Request",
      401 -> "Unauthorized",
      403 -> "Forbidden",
      404 -> "Not Found",
      418 -> "I'm a teapot",
      500 -> "Internal Server Error",
    )

  def statusMessageString(code: Int): String = statusMessage getOrElse (code, code.toString)

  val Continue = 100
  val `Switching Protocols` = 101
  val Processing = 102
  val `Early Hints` = 103
  val OK = 200
  val Created = 201
  val Accepted = 202
  val `Non-Authoritative Information` = 203
  val `No Content` = 204
  val `Moved Permanently` = 301
  val `Not Modified` = 304
  val `Bad Request` = 400
  val Unauthorized = 401
  val Forbidden = 403
  val `Not Found` = 404
  val `I'm a teapot` = 418
  val `Internal Server Error` = 500
