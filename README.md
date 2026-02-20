# spritz

![Maven Central](https://img.shields.io/maven-central/v/io.github.edadma/spritz_native0.5_3)
[![Last Commit](https://img.shields.io/github/last-commit/spritzsn/spritz)](https://github.com/spritzsn/spritz/commits)
![GitHub](https://img.shields.io/github/license/spritzsn/spritz)
![Scala Version](https://img.shields.io/badge/Scala-3.8.1-blue.svg)
![Scala Native Version](https://img.shields.io/badge/Scala_Native-0.5.10-blue.svg)

An Express.js-style HTTP framework for Scala Native, powered by libuv.

## Overview

Spritz provides a familiar routing and middleware API for building HTTP servers that compile to native executables. Features include:

- **Express-style routing** - `get`, `post`, `put`, `delete` with path parameters
- **Middleware** - composable request/response pipeline
- **JSON responses** - built-in JSON serialization
- **Virtual hosting** - route by hostname
- **Async support** - `Future`-based with `async`/`await` via dotty-cps-async
- **Content type detection** - automatic from file extensions
- **Response timing** - built-in middleware

## Prerequisites

- JDK 11 or higher
- sbt 1.12+
- LLVM/Clang
- libuv development library (`apt install libuv1-dev` / `brew install libuv`)

## Usage

Add to your `build.sbt`:

```scala
libraryDependencies += "io.github.edadma" %%% "spritz" % "0.1.0"
```

### Hello World

```scala
import io.github.spritzsn.spritz.*

@main def run(): Unit =
  val app = new Server("MyApp")

  app.get("/") { (req, res) =>
    res.send("Hello, World!")
  }
  println("Listening on port 3000")
  app.listen(3000)
```

### JSON API

```scala
import io.github.spritzsn.spritz.*

@main def run(): Unit =
  val app = new Server()

  app.get("/api/users/:id") { (req, res) =>
    val id = req.params.selectDynamic("id")
    res.json(Map("id" -> id, "name" -> "Alice"))
  }
  app.listen(3000)
```

### Middleware

```scala
import io.github.spritzsn.spritz.*

@main def run(): Unit =
  val app = new Server()

  app.use(responseTime)
  app.use { (req, res) =>
    println(s"${req.method} ${req.originalPath}")
    HandlerResult.Next
  }
  app.get("/") { (req, res) =>
    res.send("Hello!")
  }
  app.listen(3000)
```

## Building

```bash
sbt compile
sbt run
sbt nativeLink    # build native executable
```

## License

ISC
