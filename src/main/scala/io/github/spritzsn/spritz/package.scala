package io.github.spritzsn.spritz

import scala.concurrent.ExecutionContext

implicit val loop: ExecutionContext = io.github.spritzsn.async.loop
