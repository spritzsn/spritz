package io.github.spritzsn.spritz

import cps.*
import cps.monads.FutureAsyncMonad

import scala.concurrent.duration.*
import io.github.spritzsn.async.*
import io.github.spritzsn.libuv.defaultLoop

def responseTime(req: Request, res: Response): HandlerResult =
  val start = defaultLoop.now

  res.actions += (_.set("X-Response-Time", defaultLoop.now - start))
  HandlerResult.Next

def cors(req: Request, res: Response): HandlerResult =
  res.actions += (_.set("Access-Control-Allow-Origin", "*"))
  HandlerResult.Next

@main def run(): Unit =
  Server { app =>
    app use responseTime
    app use cors
    app.get(
      "/",
      (req, res) =>
        async {
          await(timer(.5 second))
          res.send("hello world") /*status(HTTP.`No Content`)*/
        },
    )
    app.listen(3000, "TestServer/1.0")
    println("listening")
  }

//  async {
//    for i <- 1 to 3 do
//      println(i)
//      await(Timer(.5 second))
//  }

//  Server { app =>
//    app.get("/", (req, res) => res.send("hello"))
//    app.listen(3000)
//    println("listening")
//  }
