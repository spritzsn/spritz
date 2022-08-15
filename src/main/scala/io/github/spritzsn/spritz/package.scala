package io.github.spritzsn

import io.github.spritzsn.async.EventLoop

package object spritz:
  implicit val loop: EventLoop.type = EventLoop
