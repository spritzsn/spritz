package io.github.spritzsn.spritz

import scala.collection.mutable

object DMap extends Dynamic:
  def applyDynamicNamed(name: String)(mapping: (String, Any)*): DMap = new DMap(mutable.HashMap(mapping: _*))

class DMap(val m: mutable.HashMap[String, Any] = new mutable.HashMap) extends mutable.Map[String, Any] with Dynamic:
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
