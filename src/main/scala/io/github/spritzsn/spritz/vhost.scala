package io.github.spritzsn.spritz

def vhost(hostname: String, handler: RequestHandler): RequestHandler =
  var end = 0
  val buf = new StringBuilder

  "\\*".r
    .findAllMatchIn(hostname)
    .foreach { m =>
      buf ++= (if end < m.start then s"\\Q${hostname.substring(end, m.start)}\\E.+" else ".+")
      end = m.end
    }
  if end < hostname.length then buf ++= s"\\Q${hostname.substring(end)}\\E"

  val regex = buf.toString.r

  (req, res) => if regex.matches(req.hostname) then handler(req, res) else HandlerResult.Next
