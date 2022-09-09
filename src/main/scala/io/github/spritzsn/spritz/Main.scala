package io.github.spritzsn.spritz

import cps.*
import cps.monads.FutureAsyncMonad

@main def run(): Unit =
  Server("TestServer/1") { app =>
    app
      .use(responseTime())
      .use("/project", (req, res) => res.send("ok"))
      .get(
        "/",
        (req, res) => { println(req); HandlerResult.Next },
        (req, res) => async { res.json(req.query, 2) },
      )
      .get("/a", (req, res) => res.send("a route"))
    app.listen(3000)
    println("listening")
  }
