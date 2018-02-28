package com.sct.stream.graph

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl._
import akka.stream._

import scala.collection.immutable

//case class RetryShape[In, Out](in: Inlet[In],
//                               retry: Inlet[In],
//                               out: Outlet[Out])
//    extends Shape {
//
//  // It is important to provide the list of all input and output
//  // ports with a stable order. Duplicates are not allowed.
//  override val inlets: immutable.Seq[Inlet[_]] =
//    in :: retry :: Nil
//  override val outlets: immutable.Seq[Outlet[_]] =
//    out :: Nil
//
//  // A Shape must be able to create a copy of itself. Basically
//  // it means a new instance with copies of the ports
//  override def deepCopy() =
//    RetryShape(in.carbonCopy(),
//               retry.carbonCopy(),
//               out.carbonCopy())
//
//  override def copyFromPorts(
//      inlets: immutable.Seq[Inlet[_]],
//      outlets: immutable.Seq[Outlet[_]]): RetryShape[In, Out] = {
//    assert(inlets.size == this.inlets.size)
//    assert(outlets.size == this.outlets.size)
//    // This is why order matters when overriding inlets and outlets.
//
//    RetryShape[In, Out](inlets.head.as[In],
//                        inlets(1).as[In],
//                        outlets.head.as[Out])
//  }
//}

object RetryShape {

  final case class Retryable[In](value: In, retry: Int)
  object Retryable {
    def apply[In](retryable: Retryable[In]): Retryable[In] = Retryable(retryable.value, retryable.retry + 1)
  }

  def apply[In, Out](worker: Flow[In, Either[In, Out], Any], retryies: Int)
    : Graph[FlowShape[In, Out], NotUsed] = {

    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val partition = builder.add(Partition[Either[In, Out]](2, x => if (x.isRight) 0 else 1))

      // Ensure only right will be outed
      val input = builder.add(worker)
      val rightOut = builder.add(Flow[Either[In, Out]].collect { case Right(x) => x })
      val leftOut = builder.add(Flow[Either[In, Out]].collect { case Left(x) => x })

      // After merging priority and ordinary jobs, we feed them to the balancer
      input ~> partition.in

      // Handle ok
      partition.out(0) ~> rightOut

      // Handle errors
      partition.out(1) ~> leftOut ~> input

      // We now expose the input ports of the priorityMerge and the output
      // of the resultsMerge as our PriorityWorkerPool ports
      // -- all neatly wrapped in our domain specific Shape
      FlowShape(in = input.in, out = rightOut.out)
    }
  }
}

object RetryShapeMain extends App {

  implicit val system = ActorSystem("RetryShape")
  implicit val materializer = ActorMaterializer()

  val worker: Flow[Int, Either[Int, String], NotUsed] =
    Flow[Int].map { x =>
      println("Processing: " + x)
      if (x % 2 == 0) Left(x) else Right("step 1 " + x)
    }

  val flow = Flow.fromGraph(RetryShape(worker, 5))

  Source.fromIterator(() => Iterator(1, 2, 3, 4, 5, 6, 7, 8)).via(flow).runWith(Sink.foreach(println))

  Thread.sleep(1000)
  system.terminate()
}

/*
object RetryShapeMain extends App {

  implicit val system = ActorSystem("RetryShape")
  implicit val materializer = ActorMaterializer()

  val worker1 = Flow[String].map("step 1 " + _)
  val worker2 = Flow[String].map("step 2 " + _)

  RunnableGraph.fromGraph(GraphDSL.create(){ implicit b =>
    import GraphDSL.Implicits._

    val priorityPool1 = b.add(RetryShape(worker1, 4))
    val priorityPool2 = b.add(RetryShape(worker2, 2))

    Source(1 to 10).map("job: " + _) ~> priorityPool1.jobsIn
    Source(1 to 10).map("priority job: " + _) ~> priorityPool1.priorityJobsIn

    priorityPool1.resultsOut ~> priorityPool2.jobsIn
    Source(1 to 10).map("one-step, priority " + _) ~> priorityPool2.priorityJobsIn

    priorityPool2.resultsOut ~> Sink.foreach(println)
    ClosedShape
  }).run()

  Thread.sleep(1000)
  system.terminate()
}
 */
