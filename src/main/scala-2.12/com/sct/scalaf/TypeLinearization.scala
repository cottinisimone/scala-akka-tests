package com.sct.scalaf

import scala.collection.mutable.ArrayBuffer

abstract class IntQueue {
  def get(): Int
  def put(x: Int)
}

class BasicIntQueue extends IntQueue {
  private val buf = new ArrayBuffer[Int]
  def get() = buf.remove(0)
  def put(x: Int) = {
    println("Putting")
    buf += x
  }
}

trait Doubling extends IntQueue {
  abstract override def put(x: Int) = { super.put(2 * x) }
}

trait Incrementing extends IntQueue {
  abstract override def put(x: Int) = {
    println("Incrementing")
    super.put(x + 1)
  }
}

trait Filtering extends IntQueue {
  abstract override def put(x: Int) = {
    println("Filtering")
    if (x >= 0) super.put(x)
  }
}

object TypeLinearization extends App {
  val queue1 = new BasicIntQueue with Incrementing with Filtering
  val queue2 = new BasicIntQueue with Filtering with Incrementing

  queue1.put(-1)
  queue1.put(0)
  queue1.put(1)

  queue2.put(-1)
  queue2.put(0)
  queue2.put(1)

  println(queue1.get())
  println(queue1.get())
  println(queue2.get())
  println(queue2.get())
  println(queue2.get())
}


