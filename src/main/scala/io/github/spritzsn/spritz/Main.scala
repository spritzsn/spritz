package io.github.spritzsn.spritz

import cps.*
import cps.monads.FutureAsyncMonad

@main def run(): Unit =
  val app = new Server("TestServer/1")

  app.get(
    "/",
    (req, res) => res.send("hello"),
  )
  app.listen(3000)
  println("listening")
  app.run()
