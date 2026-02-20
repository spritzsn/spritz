package io.github.spritzsn.spritz

import cps.*
import cps.monads.FutureAsyncMonad

@main def run(): Unit =
  val app = new Server("TestServer/1")

  app.get("/", (_, res) => res.send("hello"))
  println("listening on port 3000")
  app.listen(3000)
