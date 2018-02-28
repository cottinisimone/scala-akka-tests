package com.sct.stream

import akka.actor.ActorSystem
import akka.stream.Supervision.{Directive, Restart, Resume, Stop}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, MergePreferred, Sink, Source, Zip}
import akka.stream.{ActorMaterializer, FlowShape, Inlet, Outlet, Shape}

import scala.collection.immutable
import scala.util.control.NonFatal

object ErrorHandling extends App {

  implicit val system = ActorSystem("ErrorHandling")
  implicit val materializer = ActorMaterializer()

  val list = Iterator(0, 1, 2, 3, 4)

  val supervisionStrategy: Function[Throwable, Directive] = {
    case _: ArithmeticException => Resume
    case NonFatal(_) => Restart
    case _ => Stop
  }

  val throwFlow = Flow[Int].map(4 / _).recover {
    case x => x
  }
  //    .withAttributes(ActorAttributes.supervisionStrategy(supervisionStrategy))

  import GraphDSL.Implicits._
  val partial = GraphDSL.create() { implicit builder =>
    val A = builder.add(Broadcast[Int](2))
    val B = builder.add(Zip[Int, String]())

    A ~> Flow[Int] ~> B.in0
    A ~> Flow[Int].map(_.toString + "ciao") ~> B.in1

    FlowShape(A.in, B.out)
  }

  import GraphDSL.Implicits._
//  val mergePriority = GraphDSL.create() { implicit builder =>
//    val M = builder.add(MergePreferred[Int](1))
//    val I = Inlet[Int]
//    val O = Outlet[Int]
//
//    M ~> Flow[Int].log("Cazzu") ~> O
//    I ~> Flow[Int].map(_.toString + "ciao") ~> B.in1
//
//    FlowShape(I, O)
//  }

  Source
    .fromIterator[Int](() => list)
    .via(Flow.fromGraph(partial))
    .runWith(Sink.foreach(println))
}

//case class PriorityWorkerPoolShape[In, Out](jobsIn: Inlet[In],
//                                            priorityJobsIn: Inlet[In],
//                                            resultsOut: Outlet[Out])
//    extends Shape {
//
//  // It is important to provide the list of all input and output
//  // ports with a stable order. Duplicates are not allowed.
//  override val inlets: immutable.Seq[Inlet[_]] =
//    jobsIn :: priorityJobsIn :: Nil
//  override val outlets: immutable.Seq[Outlet[_]] =
//    resultsOut :: Nil
//
//  // A Shape must be able to create a copy of itself. Basically
//  // it means a new instance with copies of the ports
//  override def deepCopy() =
//    PriorityWorkerPoolShape(jobsIn.carbonCopy(),
//                            priorityJobsIn.carbonCopy(),
//                            resultsOut.carbonCopy())
//
//  override def copyFromPorts(inlets: immutable.Seq[Inlet[_]], outlets: immutable.Seq[Outlet[_]]): Shape = ???
//}
