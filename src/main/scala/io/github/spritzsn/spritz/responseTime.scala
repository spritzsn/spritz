package io.github.spritzsn.spritz

import io.github.spritzsn.libuv.hrTime

def responseTime(digits: Int = 3, suffix: Boolean = true): RequestHandler =
  (req, res) =>
    val start = hrTime

    res action { res.set("X-Response-Time", responseTime(start, digits, suffix)) }
    HandlerResult.Next

def responseTime(start: Long, digits: Int, suffix: Boolean): String =
  s"%.${digits}f${if suffix then "ms" else ""}".format((hrTime - start) / 1000 / 1000d)
