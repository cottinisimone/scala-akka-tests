package com.sct.actors

import java.io.File

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.event.LoggingReceive
import akka.persistence.{PersistentActor, RecoveryCompleted, SnapshotOffer}
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._

object ActorUnhandled extends App {

  val newConfig = ConfigFactory.parseFile(new File("src/main/resources/inmemory.conf"))

  val system = ActorSystem("test", ConfigFactory.load(newConfig))

  val actor = system.actorOf(Props(new ActorUnhandled), "test")

  actor ! Handled
  actor ! NotHandled
  actor ! "Ciao"
}

class ActorUnhandled extends PersistentActor with ActorLogging {

  override def persistenceId: String = "AU" + self.path.name

  context.setReceiveTimeout(1.seconds)

  override def receiveCommand: Receive = LoggingReceive {
    case Handled =>
      println("handled received")
  }

  override def unhandled(message: Any): Unit = message match {
    case NotHandled =>
      println("nothandled received")

    case a =>
      println("other not handled message " + a)
      super.unhandled(message)
  }

  override def receiveRecover: Receive = {
    case Handled => println("Handled recovered")
    case NotHandled => println("NotHandled recovered")

    case _: RecoveryCompleted =>
      log.debug("ReceiveRecover: RecoveryCompleted")

    case SnapshotOffer(_, _) =>
      log.debug("ReceiveRecover: SnapshotOffer")

    case _ => println("Unrecognize")
  }

}

case object Handled
case object NotHandled