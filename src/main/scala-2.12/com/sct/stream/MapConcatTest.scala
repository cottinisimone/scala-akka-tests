package com.sct.stream

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.ExecutionContext.Implicits.global

object MapConcatTest extends App {

  implicit val system = ActorSystem("Concat")
  implicit val materializer = ActorMaterializer()

  case class Rangee(id: String, lower: Long, upper: Long)

  val list = List(Rangee("ean1", 1, 100),
                  Rangee("ean2", 101, 267),
                  Rangee("ean3", 268, 356),
                  Rangee("ean4", 357, 408),
                  Rangee("ean5", 409, 502),
                  Rangee("ean6", 503, 657),
                  Rangee("ean7", 658, 789),
                  Rangee("ean8", 790, 875),
                  Rangee("ean9", 876, 1000),
                  Rangee("ean10", 1001, 1320),
                  Rangee("ean11", 1321, 1487),
                  Rangee("ean12", 1488, 1549),
                  Rangee("ean13", 1550, 1915))

  def genAsync: Flow[Rangee, (String, Seq[Long]), NotUsed] = Flow[Rangee].map {
    rangee =>
      val sleepFor = 100
      val start = System.currentTimeMillis()
      while (System.currentTimeMillis() - start < sleepFor) {}
      (rangee.id, (rangee.lower to rangee.upper).toList)
  }

  def saveAsync: Flow[(String, Seq[Long]), (String, Seq[Long]), NotUsed] =
    Flow[(String, Seq[Long])].map { some =>
      val sleepFor = 500
      val start = System.currentTimeMillis()
      while (System.currentTimeMillis() - start < sleepFor) {}
      some
    }

  def balancer(generator: Flow[Rangee, (String, Seq[Long]), NotUsed],
               saver: Flow[(String, Seq[Long]), (String, Seq[Long]), NotUsed],
               parallelism: Int): Flow[Rangee, (String, Seq[Long]), NotUsed] =
    Flow.fromGraph(GraphDSL.create[FlowShape[Rangee, (String, Seq[Long])]]() {
      implicit builder: GraphDSL.Builder[NotUsed] =>
        import GraphDSL.Implicits._

        val balance = builder.add(Balance[Rangee](parallelism))
        val merge = builder.add(Merge[(String, Seq[Long])](parallelism))

        for (i <- 0 until parallelism)
          balance.out(i) ~> generator.async ~> saver ~> merge.in(i)

        FlowShape(balance.in, merge.out)
    })

  val future = Source(list)
    .mapConcat[Rangee] { rangee =>
      val range = rangee.lower to rangee.upper by 100
      val slided = (if (range.last != rangee.upper) range :+ rangee.upper
                    else range).sliding(2)
      val res = slided.map(x => Rangee(rangee.id, x.head, x.tail.head)).toList
      println(res)
      res
    }
    .via {
      balancer(genAsync, saveAsync, 8).async
    }
    .groupBy(16, x => x._1)
    .mergeSubstreamsWithParallelism(16)
    .runWith(Sink.foreach(println))

  future.onComplete { _ =>
    system.terminate()
  }
}
