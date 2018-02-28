package com.sct.actors

import akka.actor.{Actor, ActorSystem, PoisonPill, Props}
import akka.event.LoggingReceive
import akka.util.Timeout
import com.sct.actors.ActorReceiveSides.Message

import scala.concurrent.duration._

// This test purpose is to demonstrate that side function is called only first time receive method is called.
//  Other invocations of methods does not execute side function.
//  Then find the solution to avoid this problem.

class ActorReceiveSides extends Actor {

//  Wrong
//
  def side(rec: Receive): Receive = {
    println("hello side effect")
    rec
  }
//
//  override def receive: Receive = LoggingReceive {
//    side {
//      case msg: Message =>
//        println(msg.stringa)
//        sender ! Done
//    }
//  }

  private[this] val manageSnapshotSave = PartialFunction[Any, Any] { any =>
    println("snapshot offered")
    any
  }

  def withSnapshot(receive: Receive): Receive = {
    manageSnapshotSave andThen receive
  }

  // If side effects functions are inverted result is always the same, that is that side is runned only one time
  override def receive: Receive = LoggingReceive {
    withSnapshot {
      side {
        case msg: Message =>
          println(msg)
      }
    }
  }

  override def postStop: Unit = {
    context.system.terminate()
  }
}

object ActorReceiveSides extends App {

  case class Message(stringa: String)

  implicit val timeout = Timeout(1.seconds)

  val system = ActorSystem("test")
  val actor = system.actorOf(Props(new ActorReceiveSides), "test")

  actor ! Message("receive 1")
  actor ! Message("receive 2")
  actor ! Message("receive 3")

  actor ! PoisonPill
}
