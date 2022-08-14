package io.github.spritzsn.spritz

abstract class Machine:
  val start: State

  case object NOT_STARTED extends State { def on = { case _ => } }
  case object DONE extends State { def on = { case _ => } }

  var received: Int = 0
  var started = false
  var state: State = NOT_STARTED
  var idx: Int = 0
  var trace = false

  def isDone: Boolean = state == DONE

  var pushedback: Int = 0
  var full = false

  def pushback(a: Int): Unit =
    if full then sys.error(s"pushback buffer full: $pushedback")
    full = true
    pushedback = a

  protected def goto(next: State): Unit =
    next.enter()
    state = next

  def selfTransition(): Unit = transition(state)

  def transition(next: State): Unit =
    if trace then println(s"$state => $next")
    if state != null then state.exit()
    goto(next)

  def directTransition(next: State): Unit =
    if trace then println(s"$state =direct> $next")
    goto(next)

  def send(a: Int): Unit =
    def send(a: Int): Unit =
      if trace then
        println(s"$state <- $a (${if a == '\r' then "\\r" else if a == '\n' then "\\n" else a.toChar.toString})")

      state on a

    if !started then
      started = true
      transition(start)

    received += 1
    send(a)

    while full do
      full = false
      send(pushedback)
  end send

  abstract class State:
    def on: PartialFunction[Int, Unit]

    def enter(): Unit = {}
    def exit(): Unit = {}
  end State

  override def toString: String = s"machine state: $state, received: $received"
end Machine
