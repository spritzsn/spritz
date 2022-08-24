package io.github.spritzsn.spritz

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId, ZoneOffset}

def stringify(
    value: Any,
    tab: Int = 2,
    format: Boolean = false,
): String = {
  val buf = new StringBuilder
  var level = 0

  def ln(): Unit =
    if (format)
      buf += '\n'

  def indent(): Unit = {
    ln()
    level += tab
    margin()
  }

  def dedent(): Unit = {
    ln()
    level -= tab
    margin()
  }

  def margin(): Unit =
    if (format)
      buf ++= " " * level

  def aggregate[T](open: Char, seq: collection.Seq[T], close: Char)(render: T => Unit): Unit = {
    buf += open
    indent()

    val it = seq.iterator

    if (it.nonEmpty)
      render(it.next())

    while (it.hasNext) {
      buf += ','
      ln()
      margin()
      render(it.next())
    }

    dedent()
    buf += close
  }

  def jsonValue(value: Any): Unit =
    value match {
      case d: Double if d.isWhole                                  => buf ++= "%.0f" format d
      case _: Number | _: java.math.BigDecimal | _: Boolean | null => buf ++= String.valueOf(value)
      case m: collection.Map[_, _]           => jsonObject(m.toSeq.asInstanceOf[Seq[(String, Any)]])
      case s: collection.Seq[_] if s.isEmpty => buf ++= "[]"
      case s: collection.Seq[_]              => aggregate('[', s, ']')(jsonValue)
      case a: Array[_]                       => jsonValue(a.toList)
      case p: Product                        => jsonObject(p.productElementNames zip p.productIterator toList)
      case t: Instant => buf ++= '"' +: DateTimeFormatter.ISO_DATE_TIME.format(t.atOffset(ZoneOffset.UTC)) :+ '"'
      case _: String =>
        buf += '"'
        buf ++=
          List("\\" -> "\\\\", "\"" -> "\\\"", "\t" -> "\\t", "\n" -> "\\n", "\r" -> "\\r").foldLeft(value.toString) {
            case (acc, (c, r)) => acc.replace(c, r)
          }
        buf += '"'
      case _ => buf ++= '"' +: String.valueOf(value) :+ '"'
    }

  def jsonObject(pairs: Seq[(String, Any)]): Unit =
    if (pairs.isEmpty)
      buf ++= "{}"
    else
      aggregate('{', pairs, '}') { case (k, v) =>
        jsonValue(k)
        buf ++= (if (format) ": " else ":")
        jsonValue(v)
      }

  jsonValue(value)
  ln()
  buf.toString
}
