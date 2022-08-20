//package io.github.spritzsn.spritz
//
//import cps.*
//import cps.monads.FutureAsyncMonad
//
//import scala.concurrent.duration.*
//import io.github.spritzsn.async.*
//import io.github.spritzsn.libuv.{defaultLoop, hrTime}
//
//import scala.io.Codec
//
//@main def run(): Unit =
//  Server("TestServer/1") { app =>
//    app
//      .use(responseTime())
//      .use(cors())
//      .get(
//        "/",
//        (req: Request, res: Response) =>
//          async {
//            res.send(await(fs.readFile("asdf", Codec.UTF8)))
//          },
//      )
//    app.listen(3000)
//    println("listening")
//  }

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
