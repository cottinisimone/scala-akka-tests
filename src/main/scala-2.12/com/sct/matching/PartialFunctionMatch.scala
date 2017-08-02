package com.sct.matching

object PartialFunctionMatch extends App {

  val a = PartialFunction[Any, Any]  { any =>
    println("a")
    any
  }

  val b: PartialFunction[Any, Any] = {
    case _: String => println("b")
  }

  val c: PartialFunction[Any, Any] = {
    case _: Boolean => println("c")
  }

  // Working
  def foo(x: Any): Any =
    (a andThen (b orElse c)).apply(x)

  // Match error
  def foo2(x: Any): Any =
    ((a andThen b) orElse c).apply(x)

  foo("aaaa")
  foo(true)

  foo2("aaaa")
  foo2(true)
}
