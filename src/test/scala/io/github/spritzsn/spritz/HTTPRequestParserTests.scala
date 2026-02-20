package io.github.spritzsn.spritz

import org.scalatest.funsuite.AnyFunSuite

class HTTPRequestParserTests extends AnyFunSuite:
  private def parse(raw: String): HTTPRequestParser =
    val parser = new HTTPRequestParser
    raw.foreach(c => parser.send(c.toInt))
    parser

  private def parseBytes(bytes: Array[Byte]): HTTPRequestParser =
    val parser = new HTTPRequestParser
    bytes.foreach(b => parser.send(b & 0xff))
    parser

  // --- Request line parsing ---

  test("simple GET request") {
    val p = parse("GET / HTTP/1.1\r\nHost: localhost\r\n\r\n")
    assert(p.isFinal)
    assert(p.method == "GET")
    assert(p.path == "/")
    assert(p.version == "HTTP/1.1")
    assert(p.headers("Host") == "localhost")
  }

  test("GET with path") {
    val p = parse("GET /api/users HTTP/1.1\r\nHost: localhost\r\n\r\n")
    assert(p.isFinal)
    assert(p.method == "GET")
    assert(p.path == "/api/users")
  }

  test("POST method") {
    val p = parse("POST /data HTTP/1.1\r\nHost: localhost\r\n\r\n")
    assert(p.isFinal)
    assert(p.method == "POST")
  }

  test("PUT method") {
    val p = parse("PUT /item/1 HTTP/1.1\r\nHost: localhost\r\n\r\n")
    assert(p.isFinal)
    assert(p.method == "PUT")
    assert(p.path == "/item/1")
  }

  test("DELETE method") {
    val p = parse("DELETE /item/1 HTTP/1.1\r\nHost: localhost\r\n\r\n")
    assert(p.isFinal)
    assert(p.method == "DELETE")
  }

  test("PATCH method") {
    val p = parse("PATCH /item/1 HTTP/1.1\r\nHost: localhost\r\n\r\n")
    assert(p.isFinal)
    assert(p.method == "PATCH")
  }

  test("OPTIONS method") {
    val p = parse("OPTIONS * HTTP/1.1\r\nHost: localhost\r\n\r\n")
    assert(p.isFinal)
    assert(p.method == "OPTIONS")
    assert(p.path == "*")
  }

  test("HEAD method") {
    val p = parse("HEAD / HTTP/1.1\r\nHost: localhost\r\n\r\n")
    assert(p.isFinal)
    assert(p.method == "HEAD")
  }

  test("HTTP/1.0 version") {
    val p = parse("GET / HTTP/1.0\r\nHost: localhost\r\n\r\n")
    assert(p.isFinal)
    assert(p.version == "HTTP/1.0")
  }

  test("deep nested path") {
    val p = parse("GET /a/b/c/d/e/f HTTP/1.1\r\nHost: localhost\r\n\r\n")
    assert(p.isFinal)
    assert(p.path == "/a/b/c/d/e/f")
  }

  test("path with special characters") {
    val p = parse("GET /foo-bar_baz.html HTTP/1.1\r\nHost: localhost\r\n\r\n")
    assert(p.isFinal)
    assert(p.path == "/foo-bar_baz.html")
  }

  test("url captures path without query") {
    val p = parse("GET /hello HTTP/1.1\r\nHost: localhost\r\n\r\n")
    assert(p.isFinal)
    assert(p.url.toString == "/hello")
  }

  // --- Query string parsing ---

  test("single query parameter") {
    val p = parse("GET /search?q=hello HTTP/1.1\r\nHost: localhost\r\n\r\n")
    assert(p.isFinal)
    assert(p.path == "/search")
    assert(p.query.toMap == Map("q" -> "hello"))
  }

  test("multiple query parameters") {
    val p = parse("GET /search?q=hello&page=1 HTTP/1.1\r\nHost: localhost\r\n\r\n")
    assert(p.isFinal)
    assert(p.query.toMap == Map("q" -> "hello", "page" -> "1"))
  }

  test("query with empty value") {
    val p = parse("GET /search?q= HTTP/1.1\r\nHost: localhost\r\n\r\n")
    assert(p.isFinal)
    assert(p.query.toMap == Map("q" -> ""))
  }

  test("query with encoded space as +") {
    val p = parse("GET /search?q=hello+world HTTP/1.1\r\nHost: localhost\r\n\r\n")
    assert(p.isFinal)
    assert(p.query.toMap == Map("q" -> "hello world"))
  }

  test("query with percent-encoded characters") {
    val p = parse("GET /search?q=hello%20world HTTP/1.1\r\nHost: localhost\r\n\r\n")
    assert(p.isFinal)
    assert(p.query.toMap == Map("q" -> "hello world"))
  }

  test("query with encoded key") {
    val p = parse("GET /search?my%20key=value HTTP/1.1\r\nHost: localhost\r\n\r\n")
    assert(p.isFinal)
    assert(p.query.toMap == Map("my key" -> "value"))
  }

  test("query preserves order") {
    val p = parse("GET /?z=3&a=1&m=2 HTTP/1.1\r\nHost: localhost\r\n\r\n")
    assert(p.isFinal)
    val pairs = p.query.toList
    assert(pairs == List("z" -> "3", "a" -> "1", "m" -> "2"))
  }

  test("query with many parameters") {
    val p = parse("GET /?a=1&b=2&c=3&d=4&e=5 HTTP/1.1\r\nHost: localhost\r\n\r\n")
    assert(p.isFinal)
    assert(p.query.size == 5)
    assert(p.query.toMap == Map("a" -> "1", "b" -> "2", "c" -> "3", "d" -> "4", "e" -> "5"))
  }

  test("url captures full query string") {
    val p = parse("GET /search?q=hello&page=1 HTTP/1.1\r\nHost: localhost\r\n\r\n")
    assert(p.isFinal)
    assert(p.url.toString == "/search?q=hello&page=1")
  }

  test("query parameter with special characters in value") {
    val p = parse("GET /search?url=http%3A%2F%2Fexample.com HTTP/1.1\r\nHost: localhost\r\n\r\n")
    assert(p.isFinal)
    assert(p.query.toMap == Map("url" -> "http://example.com"))
  }

  // --- Header parsing ---

  test("single header") {
    val p = parse("GET / HTTP/1.1\r\nHost: localhost\r\n\r\n")
    assert(p.isFinal)
    assert(p.headers.size == 1)
    assert(p.headers("Host") == "localhost")
  }

  test("multiple headers") {
    val p = parse("GET / HTTP/1.1\r\nHost: localhost\r\nAccept: text/html\r\nUser-Agent: test\r\n\r\n")
    assert(p.isFinal)
    assert(p.headers("Host") == "localhost")
    assert(p.headers("Accept") == "text/html")
    assert(p.headers("User-Agent") == "test")
  }

  test("case-insensitive header lookup") {
    val p = parse("GET / HTTP/1.1\r\nContent-Type: text/plain\r\n\r\n")
    assert(p.isFinal)
    assert(p.headers("content-type") == "text/plain")
    assert(p.headers("CONTENT-TYPE") == "text/plain")
    assert(p.headers("Content-Type") == "text/plain")
  }

  test("header value with leading whitespace trimmed") {
    val p = parse("GET / HTTP/1.1\r\nHost:   localhost\r\n\r\n")
    assert(p.isFinal)
    assert(p.headers("Host") == "localhost")
  }

  test("header value with colon") {
    val p = parse("GET / HTTP/1.1\r\nHost: localhost:8080\r\n\r\n")
    assert(p.isFinal)
    assert(p.headers("Host") == "localhost:8080")
  }

  test("duplicate header overwrites") {
    val p = parse("GET / HTTP/1.1\r\nX-Test: first\r\nX-Test: second\r\n\r\n")
    assert(p.isFinal)
    assert(p.headers("X-Test") == "second")
  }

  test("many headers") {
    val headers = (1 to 20).map(i => s"X-Header-$i: value-$i").mkString("\r\n")
    val p = parse(s"GET / HTTP/1.1\r\n$headers\r\n\r\n")
    assert(p.isFinal)
    assert(p.headers.size == 20)
    assert(p.headers("X-Header-1") == "value-1")
    assert(p.headers("X-Header-20") == "value-20")
  }

  test("header with no space before value") {
    val p = parse("GET / HTTP/1.1\r\nX-Test:value\r\n\r\n")
    assert(p.isFinal)
    assert(p.headers("X-Test") == "value")
  }

  test("header with empty value after colon and space") {
    val p = parse("GET / HTTP/1.1\r\nX-Empty: \r\n\r\n")
    assert(p.isFinal)
    assert(p.headers("X-Empty") == "")
  }

  test("header with empty value after colon no space") {
    val p = parse("GET / HTTP/1.1\r\nX-Empty:\r\n\r\n")
    assert(p.isFinal)
    assert(p.headers("X-Empty") == "")
  }

  test("no headers (just blank line)") {
    val p = parse("GET / HTTP/1.1\r\n\r\n")
    assert(p.isFinal)
    assert(p.headers.isEmpty)
  }

  test("Content-Type header preserved exactly") {
    val p = parse("GET / HTTP/1.1\r\nContent-Type: application/json; charset=utf-8\r\n\r\n")
    assert(p.isFinal)
    assert(p.headers("Content-Type") == "application/json; charset=utf-8")
  }

  // --- Body parsing ---

  test("POST with body") {
    val body = "hello world"
    val p = parse(s"POST /data HTTP/1.1\r\nHost: localhost\r\nContent-Length: ${body.length}\r\n\r\n$body")
    assert(p.isFinal)
    assert(p.method == "POST")
    assert(p.path == "/data")
    assert(new String(p.body.toArray) == "hello world")
  }

  test("empty body with Content-Length 0") {
    val p = parse("POST /data HTTP/1.1\r\nHost: localhost\r\nContent-Length: 0\r\n\r\n")
    assert(p.isFinal)
    assert(p.body.isEmpty)
  }

  test("body with exact Content-Length") {
    val p = parse("POST / HTTP/1.1\r\nContent-Length: 5\r\n\r\nhello")
    assert(p.isFinal)
    assert(new String(p.body.toArray) == "hello")
  }

  test("large body") {
    val body = "x" * 10000
    val p = parse(s"POST / HTTP/1.1\r\nContent-Length: ${body.length}\r\n\r\n$body")
    assert(p.isFinal)
    assert(p.body.length == 10000)
    assert(new String(p.body.toArray) == body)
  }

  test("body with binary content") {
    val header = "POST / HTTP/1.1\r\nContent-Length: 4\r\n\r\n"
    val headerBytes = header.getBytes("ASCII")
    val bodyBytes = Array[Byte](0x00, 0x01, 0xff.toByte, 0x7f)
    val p = parseBytes(headerBytes ++ bodyBytes)
    assert(p.isFinal)
    assert(p.body.length == 4)
    assert(p.body(0) == 0x00.toByte)
    assert(p.body(1) == 0x01.toByte)
    assert(p.body(2) == 0xff.toByte)
    assert(p.body(3) == 0x7f.toByte)
  }

  test("body with newlines in content") {
    val body = "line1\r\nline2\r\n"
    val p = parse(s"POST / HTTP/1.1\r\nContent-Length: ${body.length}\r\n\r\n$body")
    assert(p.isFinal)
    assert(new String(p.body.toArray) == body)
  }

  test("JSON body") {
    val body = """{"key":"value","num":42}"""
    val p = parse(s"POST /api HTTP/1.1\r\nContent-Type: application/json\r\nContent-Length: ${body.length}\r\n\r\n$body")
    assert(p.isFinal)
    assert(new String(p.body.toArray) == body)
    assert(p.headers("Content-Type") == "application/json")
  }

  test("form-encoded body") {
    val body = "username=alice&password=secret"
    val p = parse(s"POST /login HTTP/1.1\r\nContent-Length: ${body.length}\r\n\r\n$body")
    assert(p.isFinal)
    assert(new String(p.body.toArray) == body)
  }

  test("body not read without Content-Length") {
    val p = parse("GET / HTTP/1.1\r\nHost: localhost\r\n\r\n")
    assert(p.isFinal)
    assert(p.body.isEmpty)
  }

  test("incomplete body does not reach FINAL") {
    val parser = new HTTPRequestParser
    val raw = "POST / HTTP/1.1\r\nContent-Length: 10\r\n\r\nhello"
    raw.foreach(c => parser.send(c.toInt))
    assert(!parser.isFinal)
    assert(parser.body.length == 5)
  }

  test("body completes exactly at Content-Length") {
    val parser = new HTTPRequestParser
    val header = "POST / HTTP/1.1\r\nContent-Length: 3\r\n\r\n"
    header.foreach(c => parser.send(c.toInt))
    assert(!parser.isFinal)

    parser.send('a'.toInt)
    assert(!parser.isFinal)
    parser.send('b'.toInt)
    assert(!parser.isFinal)
    parser.send('c'.toInt)
    assert(parser.isFinal)
    assert(new String(parser.body.toArray) == "abc")
  }

  // --- Incremental / streaming parsing ---

  test("byte-at-a-time parsing") {
    val parser = new HTTPRequestParser
    val raw = "GET / HTTP/1.1\r\nHost: localhost\r\n\r\n"
    raw.foreach(c => parser.send(c.toInt))
    assert(parser.isFinal)
    assert(parser.method == "GET")
  }

  test("not final before blank line") {
    val parser = new HTTPRequestParser
    "GET / HTTP/1.1\r\nHost: localhost".foreach(c => parser.send(c.toInt))
    assert(!parser.isFinal)
  }

  test("not final after first CRLF of blank line") {
    val parser = new HTTPRequestParser
    "GET / HTTP/1.1\r\nHost: localhost\r\n\r".foreach(c => parser.send(c.toInt))
    assert(!parser.isFinal)
  }

  // --- Reset and reuse ---

  test("reset clears all state") {
    val p = parse("POST /first HTTP/1.1\r\nHost: localhost\r\nContent-Length: 3\r\n\r\nabc")
    assert(p.isFinal)
    assert(p.path == "/first")
    assert(p.headers.size == 2)
    assert(p.body.length == 3)

    p.reset()
    assert(!p.isFinal)

    "GET /second HTTP/1.1\r\nAccept: */*\r\n\r\n".foreach(c => p.send(c.toInt))
    assert(p.isFinal)
    assert(p.method == "GET")
    assert(p.path == "/second")
    assert(!p.headers.contains("Host"))
    assert(p.headers("Accept") == "*/*")
    assert(p.body.isEmpty)
    assert(p.query.isEmpty)
  }

  test("reset after Content-Length 0") {
    val p = parse("POST / HTTP/1.1\r\nContent-Length: 0\r\n\r\n")
    assert(p.isFinal)

    p.reset()
    "GET / HTTP/1.1\r\nHost: localhost\r\n\r\n".foreach(c => p.send(c.toInt))
    assert(p.isFinal)
    assert(p.method == "GET")
  }

  test("multiple reset cycles") {
    val parser = new HTTPRequestParser
    for i <- 1 to 5 do
      s"GET /$i HTTP/1.1\r\nHost: localhost\r\n\r\n".foreach(c => parser.send(c.toInt))
      assert(parser.isFinal)
      assert(parser.path == s"/$i")
      parser.reset()
  }

  // --- Error handling ---

  test("reject bare CR in method") {
    assertThrows[RuntimeException] {
      parse("GE\rT / HTTP/1.1\r\n\r\n")
    }
  }

  test("reject bare LF in method") {
    assertThrows[RuntimeException] {
      parse("GE\nT / HTTP/1.1\r\n\r\n")
    }
  }

  test("reject empty method") {
    assertThrows[RuntimeException] {
      parse(" / HTTP/1.1\r\n\r\n")
    }
  }

  test("reject empty path") {
    assertThrows[RuntimeException] {
      parse("GET  HTTP/1.1\r\n\r\n")
    }
  }

  test("reject CR in path") {
    assertThrows[RuntimeException] {
      parse("GET /he\rllo HTTP/1.1\r\n\r\n")
    }
  }

  test("reject LF in path") {
    assertThrows[RuntimeException] {
      parse("GET /he\nllo HTTP/1.1\r\n\r\n")
    }
  }

  test("reject LF in version") {
    assertThrows[RuntimeException] {
      parse("GET / HTTP/1.\n1\r\n\r\n")
    }
  }

  test("reject LF in header value") {
    assertThrows[RuntimeException] {
      parse("GET / HTTP/1.1\r\nHost: local\nhost\r\n\r\n")
    }
  }

  test("reject query key without value (bare &)") {
    assertThrows[RuntimeException] {
      parse("GET /search?&q=hello HTTP/1.1\r\nHost: localhost\r\n\r\n")
    }
  }

  test("reject query with empty key") {
    assertThrows[RuntimeException] {
      parse("GET /search?=value HTTP/1.1\r\nHost: localhost\r\n\r\n")
    }
  }

  test("reject = in query value") {
    assertThrows[RuntimeException] {
      parse("GET /search?q=a=b HTTP/1.1\r\nHost: localhost\r\n\r\n")
    }
  }

  test("reject CR in query value") {
    assertThrows[RuntimeException] {
      parse("GET /search?q=a\rb HTTP/1.1\r\nHost: localhost\r\n\r\n")
    }
  }

  test("reject header key with CR after content") {
    assertThrows[RuntimeException] {
      parse("GET / HTTP/1.1\r\nBadKey\r\n\r\n")
    }
  }

  test("reject LF in header key") {
    assertThrows[RuntimeException] {
      parse("GET / HTTP/1.1\r\nBad\nKey: value\r\n\r\n")
    }
  }

  // --- Edge cases ---

  test("header value with multiple spaces after colon") {
    val p = parse("GET / HTTP/1.1\r\nHost:    example.com\r\n\r\n")
    assert(p.isFinal)
    assert(p.headers("Host") == "example.com")
  }

  test("path is just /") {
    val p = parse("GET / HTTP/1.1\r\n\r\n")
    assert(p.isFinal)
    assert(p.path == "/")
  }

  test("query string with no path change") {
    val p = parse("GET /?key=val HTTP/1.1\r\n\r\n")
    assert(p.isFinal)
    assert(p.path == "/")
    assert(p.query.toMap == Map("key" -> "val"))
  }

  test("query parameters with duplicate keys preserved in list") {
    val p = parse("GET /?a=1&a=2&a=3 HTTP/1.1\r\n\r\n")
    assert(p.isFinal)
    val vals = p.query.filter(_._1 == "a").map(_._2).toList
    assert(vals == List("1", "2", "3"))
  }

  test("Content-Length 1") {
    val p = parse("POST / HTTP/1.1\r\nContent-Length: 1\r\n\r\nx")
    assert(p.isFinal)
    assert(new String(p.body.toArray) == "x")
  }

  test("typical browser GET request") {
    val raw =
      "GET /index.html HTTP/1.1\r\n" +
        "Host: www.example.com\r\n" +
        "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8\r\n" +
        "Accept-Language: en-US,en;q=0.5\r\n" +
        "Accept-Encoding: gzip, deflate\r\n" +
        "Connection: keep-alive\r\n" +
        "User-Agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36\r\n" +
        "\r\n"
    val p = parse(raw)
    assert(p.isFinal)
    assert(p.method == "GET")
    assert(p.path == "/index.html")
    assert(p.headers("Host") == "www.example.com")
    assert(p.headers("Connection") == "keep-alive")
    assert(p.headers.size == 6)
  }

  test("typical POST with JSON body") {
    val body = """{"username":"alice","password":"secret123"}"""
    val raw =
      s"POST /api/login HTTP/1.1\r\n" +
        "Host: api.example.com\r\n" +
        "Content-Type: application/json\r\n" +
        s"Content-Length: ${body.length}\r\n" +
        "Authorization: Bearer abc123\r\n" +
        s"\r\n$body"
    val p = parse(raw)
    assert(p.isFinal)
    assert(p.method == "POST")
    assert(p.path == "/api/login")
    assert(p.headers("Authorization") == "Bearer abc123")
    assert(new String(p.body.toArray) == body)
  }

  test("pipelining simulation: three requests on same parser") {
    val parser = new HTTPRequestParser

    // Request 1
    "GET /one HTTP/1.1\r\nHost: localhost\r\n\r\n".foreach(c => parser.send(c.toInt))
    assert(parser.isFinal)
    assert(parser.path == "/one")
    parser.reset()

    // Request 2
    "POST /two HTTP/1.1\r\nContent-Length: 2\r\n\r\nhi".foreach(c => parser.send(c.toInt))
    assert(parser.isFinal)
    assert(parser.path == "/two")
    assert(new String(parser.body.toArray) == "hi")
    parser.reset()

    // Request 3
    "DELETE /three HTTP/1.1\r\nHost: localhost\r\n\r\n".foreach(c => parser.send(c.toInt))
    assert(parser.isFinal)
    assert(parser.method == "DELETE")
    assert(parser.path == "/three")
  }

  // --- Content-Length validation ---

  test("reject non-numeric Content-Length") {
    assertThrows[RuntimeException] {
      parse("POST / HTTP/1.1\r\nContent-Length: abc\r\n\r\n")
    }
  }

  test("reject Content-Length with mixed content") {
    assertThrows[RuntimeException] {
      parse("POST / HTTP/1.1\r\nContent-Length: 5abc\r\n\r\n")
    }
  }

  test("reject negative Content-Length") {
    assertThrows[RuntimeException] {
      parse("POST / HTTP/1.1\r\nContent-Length: -1\r\n\r\n")
    }
  }

  test("reject empty Content-Length value") {
    assertThrows[RuntimeException] {
      parse("POST / HTTP/1.1\r\nContent-Length: \r\n\r\n")
    }
  }

  test("reject Content-Length exceeding max body size") {
    assertThrows[RuntimeException] {
      parse("POST / HTTP/1.1\r\nContent-Length: 10485761\r\n\r\n")
    }
  }

  test("accept Content-Length at max body size boundary") {
    val parser = new HTTPRequestParser
    "POST / HTTP/1.1\r\nContent-Length: 10485760\r\n\r\n".foreach(c => parser.send(c.toInt))
    // Should not throw - just won't be final because we haven't sent the body
    assert(!parser.isFinal)
  }

  test("reject Content-Length with huge number (overflow)") {
    assertThrows[RuntimeException] {
      parse("POST / HTTP/1.1\r\nContent-Length: 99999999999999999999\r\n\r\n")
    }
  }

  test("Content-Length with leading whitespace accepted") {
    val p = parse("POST / HTTP/1.1\r\nContent-Length:  5\r\n\r\nhello")
    assert(p.isFinal)
    assert(new String(p.body.toArray) == "hello")
  }

  test("Content-Length with trailing whitespace accepted") {
    val p = parse("POST / HTTP/1.1\r\nContent-Length: 5 \r\n\r\nhello")
    assert(p.isFinal)
    assert(new String(p.body.toArray) == "hello")
  }

  test("Content-Length with leading and trailing whitespace accepted") {
    val p = parse("POST / HTTP/1.1\r\nContent-Length:   3  \r\n\r\nabc")
    assert(p.isFinal)
    assert(new String(p.body.toArray) == "abc")
  }

  test("reject Content-Length with only whitespace") {
    assertThrows[RuntimeException] {
      parse("POST / HTTP/1.1\r\nContent-Length:   \r\n\r\n")
    }
  }

  // --- Size limits ---

  test("reject method exceeding max length") {
    val longMethod = "A" * 17
    assertThrows[RuntimeException] {
      parse(s"$longMethod / HTTP/1.1\r\nHost: localhost\r\n\r\n")
    }
  }

  test("accept method at max length") {
    val method = "A" * 16
    val p = parse(s"$method / HTTP/1.1\r\nHost: localhost\r\n\r\n")
    assert(p.isFinal)
    assert(p.method == method)
  }

  test("reject URL exceeding max length") {
    val longPath = "/" + "a" * 8192
    assertThrows[RuntimeException] {
      parse(s"GET $longPath HTTP/1.1\r\nHost: localhost\r\n\r\n")
    }
  }

  test("accept URL at max length") {
    val longPath = "/" + "a" * 8191
    val p = parse(s"GET $longPath HTTP/1.1\r\nHost: localhost\r\n\r\n")
    assert(p.isFinal)
    assert(p.path == longPath)
  }

  test("reject URL exceeding max length via query string") {
    val longValue = "x" * 8192
    assertThrows[RuntimeException] {
      parse(s"GET /?k=$longValue HTTP/1.1\r\nHost: localhost\r\n\r\n")
    }
  }

  test("reject version exceeding max length") {
    val longVersion = "HTTP/" + "1" * 12
    assertThrows[RuntimeException] {
      parse(s"GET / $longVersion\r\nHost: localhost\r\n\r\n")
    }
  }

  test("accept version at max length") {
    val version = "A" * 16
    val p = parse(s"GET / $version\r\nHost: localhost\r\n\r\n")
    assert(p.isFinal)
    assert(p.version == version)
  }

  test("reject header name exceeding max length") {
    val longKey = "X" * 257
    assertThrows[RuntimeException] {
      parse(s"GET / HTTP/1.1\r\n$longKey: value\r\n\r\n")
    }
  }

  test("accept header name at max length") {
    val key = "X" * 256
    val p = parse(s"GET / HTTP/1.1\r\n$key: value\r\n\r\n")
    assert(p.isFinal)
    assert(p.headers(key) == "value")
  }

  test("reject header value exceeding max length") {
    val longValue = "v" * 8193
    assertThrows[RuntimeException] {
      parse(s"GET / HTTP/1.1\r\nX-Test: $longValue\r\n\r\n")
    }
  }

  test("accept header value at max length") {
    val value = "v" * 8192
    val p = parse(s"GET / HTTP/1.1\r\nX-Test: $value\r\n\r\n")
    assert(p.isFinal)
    assert(p.headers("X-Test") == value)
  }

  test("reject too many headers") {
    val headers = (1 to 101).map(i => s"X-H-$i: v$i").mkString("\r\n")
    assertThrows[RuntimeException] {
      parse(s"GET / HTTP/1.1\r\n$headers\r\n\r\n")
    }
  }

  test("accept exactly max headers") {
    val headers = (1 to 100).map(i => s"X-H-$i: v$i").mkString("\r\n")
    val p = parse(s"GET / HTTP/1.1\r\n$headers\r\n\r\n")
    assert(p.isFinal)
    assert(p.headers.size == 100)
  }

  test("reject query key exceeding max length") {
    val longKey = "k" * 257
    assertThrows[RuntimeException] {
      parse(s"GET /?$longKey=v HTTP/1.1\r\nHost: localhost\r\n\r\n")
    }
  }

  test("accept query key at max length") {
    val key = "k" * 256
    val p = parse(s"GET /?$key=v HTTP/1.1\r\nHost: localhost\r\n\r\n")
    assert(p.isFinal)
    assert(p.query.toMap == Map(key -> "v"))
  }
