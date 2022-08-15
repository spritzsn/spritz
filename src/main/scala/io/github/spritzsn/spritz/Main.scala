package io.github.spritzsn.spritz

import io.github.spritzsn.async.loop
import cps.*
import cps.monads.FutureAsyncMonad
import scala.concurrent.duration.*

@main def run(): Unit =
  Server { app =>
//    app.use("/eta", route.ETA)
    app.listen(3000)
    println("listening")
  }

  loop.run()

//  async {
//    for i <- 1 to 3 do
//      println(i)
//      await(Timer(.5 second))
//  }
