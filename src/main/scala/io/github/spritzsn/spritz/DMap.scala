package io.github.spritzsn.spritz

import scala.collection.mutable

object DMap extends Dynamic:
  def applyDynamicNamed(name: String)(mapping: (String, Any)*): DMap = new DMap(mutable.LinkedHashMap(mapping: _*))

class DMap(val m: mutable.LinkedHashMap[String, Any] = new mutable.LinkedHashMap)
    extends mutable.Map[String, Any]
    with Dynamic:
  def addOne(elem: (String, Any)): DMap.this.type =
    m addOne elem
    this

  def iterator: Iterator[(String, Any)] = m.iterator

  def get(key: String): Option[Any] = m get key

  def subtractOne(elem: String): DMap.this.type =
    m subtractOne elem
    this

  def selectDynamic(field: String): Any = m(field)

  def updateDynamic(field: String)(value: Any): Unit = m(field) = value

  private def render(a: Any): String =
    a match
      case s: String               => s"\"$a\""
      case m: collection.Map[_, _] => (m map { case (k, v) => s"${render(k)}: ${render(v)}" }).mkString("{", ", ", "}")
      case s: collection.Seq[_]    => s.map(render).mkString("[", ", ", "]")
      case _                       => String.valueOf(a)

  override def toString: String = render(m)
