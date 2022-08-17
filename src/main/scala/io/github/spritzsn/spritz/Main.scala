package io.github.spritzsn.spritz

import cps.*
import cps.monads.FutureAsyncMonad

import scala.concurrent.duration.*
import io.github.spritzsn.async.*
import io.github.spritzsn.libuv.defaultLoop

def responseTime(req: Request, res: Response): HandlerResult =
  val start = System.nanoTime()

  res.actions += (_.headers("X-Response-Time") = f"${(System.nanoTime() - start) / 1000 / 1000d}%.3fms")
  HandlerResult.Next

def cors(req: Request, res: Response): HandlerResult =
  res.headers("Access-Control-Allow-Origin") = "*"
  HandlerResult.Next

@main def run(): Unit =
  Server { app =>
    app use responseTime
    app use cors
    app.get("/", (req, res) => res.send("hello world"))
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
