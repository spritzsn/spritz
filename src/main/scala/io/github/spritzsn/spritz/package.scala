package io.github.spritzsn.spritz

import io.github.spritzsn.async.EventLoop

import scala.concurrent.ExecutionContext

implicit val loop: ExecutionContext = EventLoop
