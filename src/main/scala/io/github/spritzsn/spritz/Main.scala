package io.github.spritzsn.spritz

@main def run(): Unit =
  Server("TestServer/1") { app =>
    app
      .get(
        "/",
        responseTime(),
        (req: Request, res: Response) => res.json(DMap.literal(asdf = 123, zxcv = Seq("wert", true)), 2),
      )
    app.listen(3000)
    println("listening")
  }
