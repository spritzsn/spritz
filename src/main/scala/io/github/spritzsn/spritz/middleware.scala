package io.github.spritzsn.spritz

import io.github.spritzsn.libuv.hrTime

def responseTime(): RequestHandler =
  (req: Request, res: Response) =>
    val start = hrTime

    res.action(_.headers("X-Response-Time") = f"${(hrTime - start) / 1000 / 1000d}%.3fms")
    HandlerResult.Next
