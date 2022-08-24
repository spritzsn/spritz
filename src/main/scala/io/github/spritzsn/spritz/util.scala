package io.github.spritzsn.spritz

import java.nio.ByteBuffer
import scala.collection.mutable.ArrayBuffer
import scala.io.Codec

def urlDecode(s: String, codec: Codec = Codec.UTF8): String =
  if s.indexOf('%') == -1 && s.indexOf('+') == -1 then s
  else
    val bytes = new ArrayBuffer[Byte]
    var idx = 0

    def hex(d: Char): Int =
      if '0' <= d && d <= '9' then d - '0'
      else if 'A' <= d && d <= 'F' then d - 'A' + 10
      else if 'a' <= d && d <= 'f' then d - 'a' + 10
      else sys.error(s"invalid hex digit")

    while idx < s.length do
      s(idx) match
        case '%' =>
          bytes += ((hex(s(idx + 1)) << 4) + hex(s(idx + 2))).toByte
          idx += 2
        case '+' => bytes += ' '.toByte
        case c   => bytes += c.toByte

      idx += 1

    codec.charSet.decode(ByteBuffer.wrap(bytes.toArray)).toString
end urlDecode
