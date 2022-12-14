package io.github.spritzsn.spritz

import cps.*
import cps.monads.FutureAsyncMonad

@main def run(): Unit =
  val app = new Spritz("TestServer/1")

  app.get("/", (_, res) => res.send("hello"))
  println("listening no port 3000")
  app.listen(3000)
