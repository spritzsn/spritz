package io.github.spritzsn.spritz

import cps.*
import cps.monads.FutureAsyncMonad
import scala.concurrent.duration.*
import io.github.spritzsn.async._

@main def run(): Unit =
//  Server { app =>
//    app.get("/", (req, res) => res.send("hello world"))
//    app.listen(3000)
//    println("listening")
//  }

//  async {
//    for i <- 1 to 3 do
//      println(i)
//      await(Timer(.5 second))
//  }

  Server { app =>
    app.get(
      "/",
      (req, res) =>
        async {
          res.send(await(spawn("/home/ed/dev-sn/test/target/scala-3.1.3/test-out", Vector("3", "4"))))
        },
    )
    app.post("/", (req, res) => res.send(req.body))
    app.listen(3000)
    println("listening")
  }
