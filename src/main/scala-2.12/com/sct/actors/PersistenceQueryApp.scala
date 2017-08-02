package com.sct.actors

import akka.actor.{ActorSystem, Props}
import akka.persistence.PersistentActor
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.PersistenceQuery
import akka.stream.scaladsl.Sink
import akka.stream.{ActorAttributes, ActorMaterializer, Supervision}

import scala.concurrent.{ExecutionContext, Future}

object PersistenceQueryApp extends App {

  val system = ActorSystem("PersistenceQuerySystem")
  implicit lazy val executionContext = system.dispatcher
  lazy val readJournal = PersistenceQuery(system).readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)

  val myActor = system.actorOf(MyAttore.props(readJournal), "myattore")

  myActor ! SetUsername
  myActor ! SetDevice
  myActor ! SetUsername
  myActor ! CreateConversation
  myActor ! SetDevice
}

class MyAttore(readJournal: CassandraReadJournal)(implicit ec: ExecutionContext) extends PersistentActor {

  implicit val materializer = ActorMaterializer()
  private val resumingLoggingDecider: Supervision.Decider = { e: Throwable =>
    println("Exception thrown during stream processing", e)
    Supervision.Resume
  }
  protected val resumingLoggingStrategy = ActorAttributes.supervisionStrategy(resumingLoggingDecider)

  val selfie = context.actorSelection("user/myattore")
  println(selfie)

  println("Last sequence number: " + lastSequenceNr)

  override def persistenceId: String = "myattore"

  override def receiveCommand: Receive = {
    case x => persist(x) { x => println(s"persisted [$x]") }
  }

  override def receiveRecover: Receive = {
    case x => println("something wrong")
  }

  readJournal.eventsByPersistenceId("myattore", 0L, Long.MaxValue)
    .mapAsync(1) { e => Future(e.event match {
        case event: UserEvent => println(event)
        case event: DeviceEvent => println(event)
        case _ => println("Unknown")
      })
    }
    .withAttributes(resumingLoggingStrategy)
    .runWith(Sink.ignore)
}

object MyAttore {
  def props(readJournal: CassandraReadJournal)(implicit ec: ExecutionContext) = Props(new MyAttore(readJournal)(ec))
}


trait DomainEvent
trait UserEvent extends DomainEvent
trait DeviceEvent extends DomainEvent
trait ConversationEvent extends DomainEvent
case object SetUsername extends UserEvent
case object SetPhoneNumber extends UserEvent
case object SetDevice extends DeviceEvent
case object CreateConversation extends ConversationEvent