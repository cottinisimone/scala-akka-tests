package com.sct.stream

import akka.stream.FlowShape
import akka.stream.scaladsl.{Flow, GraphDSL, Partition, Sink}
import akka.{Done, NotUsed}

import scala.concurrent.Future

object PipeFlow {
  def apply[A, B](implicit left: Sink[A, Future[Done]]): Flow[Either[A, B], B, NotUsed] =
    Flow.fromGraph(
      GraphDSL.create[FlowShape[Either[A, B], B]]() { implicit builder: GraphDSL.Builder[NotUsed] =>
        import GraphDSL.Implicits._
        val right     = builder.add(Flow[Either[A, B]].collect { case Right(x) => x })
        val partition = builder.add(Partition[Either[A, B]](2, _.map(_ => 1).getOrElse(0)))

        partition.out(0) ~> Flow[Either[A, B]].collect { case Left(x) => x } ~> left
        partition.out(1) ~> right.in

        FlowShape(partition.in, right.out)
      }
    )
}
