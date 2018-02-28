package com.sct.stream

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, RestartFlow, Sink, Source}

import scala.concurrent.duration._

object RestartBackoff extends App {

  implicit val system = ActorSystem("RetryShape")
  implicit val materializer = ActorMaterializer()

  val source = Source.fromIterator(() => Iterator(1, 2, 3, 4, 10, 2, 1, 2, 2))


  case class Env[T](value: T)

  val subflow = Flow[Int].map(_.toString + "°")

//  val flow: Flow[Env[Int], Env[String], NotUsed] = Flow[Env[Int]].join(subflow)


//
//  val flow = Flow[Int].map { x =>
//    println("Processing " + x)
//    x / x
//  }
//
//  val restartFlow = RestartFlow.withBackoff(
//    minBackoff = 3.seconds,
//    maxBackoff = 30.seconds,
//    randomFactor = 0.2 // adds 20% "noise" to vary the intervals slightly
//  )(() => flow)
//
//  source.via(restartFlow).runWith(Sink.foreach(println))

}
