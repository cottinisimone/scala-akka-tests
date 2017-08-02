package com.sct.actors

import java.io.File

import akka.actor.{ActorRef, ActorSystem, DiagnosticActorLogging, Props}
import akka.cluster.sharding.ShardRegion.HashCodeMessageExtractor
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.event.LoggingReceive
import akka.persistence._
import akka.util.Timeout
import com.softwaremill.macwire.wire
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.util.Random

// This test has been done because of a null pointer exception while loading last snapshot
//    CassandraSnapshotStore (105, 107)
//      val row = rs.one() probably returns null
//      row.getBytes("snapshot") throws NullPointerException
//
// Not able to replicate exception....
object ActorSnapshottingTest extends App {

  val newConfig = ConfigFactory.parseFile(new File("src/main/resources/cluster.conf"))

  val system = ActorSystem("test", ConfigFactory.load(newConfig))

  val cluster: ActorRef = ClusterSharding(system)
    .start(
      typeName = "cluster",
      entityProps = Props(wire[ActorSnapshottingTest]),
      settings = ClusterShardingSettings(system),
      messageExtractor = wire[ActorMessageExtractor]
    )

  implicit val timeout = Timeout(1.seconds)

  for (x <- 1 to 100) {
    cluster ! Something(x, "receive 1")
    cluster ! Something(x, "receive 2")
    cluster ! Something(x, "receive 3")
    cluster ! Something(x, "receive 4")
  }
}

class ActorSnapshottingTest extends PersistentActor with DiagnosticActorLogging with ActorSnapshotting[Option[ActorState]] {

  override lazy val maxEventsWithoutSnapshot: Int = 5

  override def persistenceId: String = "AS" + self.path.name

  override var state: Option[ActorState] = Some(ActorState("fuckka"))

  override def receiveCommand: Receive = LoggingReceive {
    withSnapshot {
      manageSnapshots orElse {
        case x: Something =>
          persist(x) {
            x => println(s"persisted [$x]")
          }
      }
    }
  }

  override def receiveRecover: Receive = {
    case _: Something => println("Recovering")

    case _: RecoveryCompleted =>
      log.debug("ReceiveRecover: RecoveryCompleted")

    case SnapshotOffer(_, snapshot: Option[ActorState]) =>
      log.debug("ReceiveRecover: SnapshotOffer")
      state = snapshot

    case _ => println("Unrecognize")
  }
}

class ActorMessageExtractor extends HashCodeMessageExtractor(10) {
  override def entityId(message: Any): String = message match {
    case msg: Something => msg.userId.toString
  }
}

case class ActorState(valore1: String, valore2: List[Int] = List.fill(100000)(Random.nextInt(30)))

case class Something(userId: Int, text: String)

trait ActorSnapshotting[T] { this: PersistentActor with DiagnosticActorLogging =>

  var state: T

  def maxEventsWithoutSnapshot: Int

  def manageSnapshots: Receive = {
    case SaveSnapshotSuccess(metadata) =>
      log.debug("SaveSnapshotSuccess")
      deleteOldSnapshots(metadata)

    case SaveSnapshotFailure(metadata, reason) => log.error(reason, "SaveSnapshotFailure with metadata {}", metadata)
    case DeleteSnapshotsSuccess(criteria) => log.debug("DeleteSnapshotsSuccess, with criteria: {}", criteria)
    case DeleteSnapshotsFailure(criteria, cause) => log.error(cause, "DeleteSnapshotsFailure with criteria: {}", criteria)
  }

  private def deleteOldSnapshots(currentMetadata: SnapshotMetadata) = {
    deleteSnapshots(SnapshotSelectionCriteria(currentMetadata.sequenceNr - 1))
  }

  def withSnapshot(receive: Receive): Receive = {
    if (lastSequenceNr % maxEventsWithoutSnapshot == 0) {
      log.debug("Asking snapshot for event {}", lastSequenceNr)
      saveSnapshot(state)
    }
    receive
  }
}
