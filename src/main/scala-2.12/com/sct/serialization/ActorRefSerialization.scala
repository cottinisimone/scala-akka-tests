package com.sct.serialization

import akka.actor.{Actor, ActorSystem, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider, Props}
import akka.serialization.Serialization

// This test demonstrate how to correctly serialize and deserialize and actor ref
object ActorRefSerialization extends App {

  val system = ActorSystem("test")

  val anonymActor = system.actorOf(Props(new Actor {
    override def receive: Receive = {
      case x => println(x)
    }
  }))

  val serialized = Serialization.serializedActorPath(anonymActor)

  println(serialized)
  println(DeserializeProvider.get(system).deserialize(serialized))
}

class DeserializeProvider(actorSystem : ExtendedActorSystem) extends Extension {
  def deserialize( path : String) = actorSystem.provider.resolveActorRef(path)
}

object DeserializeProvider extends ExtensionId[DeserializeProvider] with ExtensionIdProvider {
  override def lookup = DeserializeProvider

  override def createExtension(system: ExtendedActorSystem) = new DeserializeProvider(system)

  override def get(system: ActorSystem): DeserializeProvider = super.get(system)
}


