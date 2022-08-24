package io.github.spritzsn.spritz

@main def run(): Unit =
  Server("TestServer/1") { app =>
    app
      .get(
        "/",
        (req: Request, res: Response) => res.json(req.query),
      )
    app.listen(3000)
    println("listening")
  }
