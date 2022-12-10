package io.github.spritzsn.spritz

import scala.util.matching.Regex

def vhost(hostname: String | Regex, handler: RequestHandler): RequestHandler =
  val regex =
    hostname match
      case s: String =>
        var end = 0
        val buf = new StringBuilder

        "\\*".r
          .findAllMatchIn(s)
          .foreach { m =>
            buf ++= (if end < m.start then s"\\Q${s.substring(end, m.start)}\\E.+" else ".+")
            end = m.end
          }
        if end < s.length then buf ++= s"\\Q${s.substring(end)}\\E"
        buf.toString.r
      case r: Regex => r

  (req, res) => if regex.matches(req.hostname) then handler(req, res) else HandlerResult.Next
