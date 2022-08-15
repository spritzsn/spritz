//package io.github.spritzsn.spritz
//
//import cps.*
//import cps.monads.FutureAsyncMonad
//import scala.concurrent.duration.*
//import io.github.spritzsn.async._
//
//@main def run(): Unit =
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
