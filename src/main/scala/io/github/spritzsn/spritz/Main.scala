package io.github.spritzsn.spritz

import cps.*
import cps.monads.FutureAsyncMonad

@main def run(): Unit =
  Server("TestServer/1") { app =>
    app.get("/", (req, res) => res.send("hello"))
    app.listen(3000)
    println("listening")
  }
