package com.sct.stream

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape}
import akka.stream.scaladsl.{GraphDSL, RunnableGraph, Sink, Source, ZipWith}

object BackpressureApp extends App {

  import scala.concurrent.duration._
  case class Tick()

  implicit val system = ActorSystem("ErrorHandling")
  implicit val materializer = ActorMaterializer()

  RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._

    // this is the asynchronous stage in this graph
    val zipper = b.add(ZipWith[Tick, Int, Int]((tick, count) => count).async)

    Source.tick(initialDelay = 3.second, interval = 3.second, Tick()) ~> zipper.in0

    Source.tick(initialDelay = 1.second, interval = 1.second, "message!")
      .conflateWithSeed(seed = (_) => 1)((count, _) => count + 1) ~> zipper.in1

    zipper.out ~> Sink.foreach(println)
    ClosedShape
  }).run()
}
