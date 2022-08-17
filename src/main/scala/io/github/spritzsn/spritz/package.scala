package io.github.spritzsn

import io.github.spritzsn.async.EventLoop

package object spritz:
  implicit val loop: EventLoop.type = EventLoop

  def responseTime(): RequestHandler =
    (req: Request, res: Response) =>
      val start = System.nanoTime()

      res.action(_.headers("X-Response-Time") = f"${(System.nanoTime() - start) / 1000 / 1000d}%.3fms")
      HandlerResult.Next

  def cors(): RequestHandler =
    (req: Request, res: Response) =>
      res.headers("Access-Control-Allow-Origin") = "*"
      HandlerResult.Next
