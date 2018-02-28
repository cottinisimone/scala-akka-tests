package com.sct.stream.graph

import akka.NotUsed
import akka.stream.scaladsl.Flow
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}

private case class Retryable[In](value: In, retry: Int)
private object Retryable {
  def apply[In](retryable: Retryable[In]): Retryable[In] = Retryable(retryable.value, retryable.retry + 1)
}

class RetryGraph[In, Out](worker: Flow[In, Out, NotUsed], retries: Int) extends GraphStage[FlowShape[In, Out]] {

  private[this] val in: Inlet[In] = Inlet[In]("retry.graph.in")
  private[this] val retry: Inlet[Retryable[In]] = Inlet[Retryable[In]]("retry.graph.retry")
  private[this] val out: Outlet[Out] = Outlet[Out]("retry.graph.out")

  override val shape: FlowShape[In, Out] = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = {
    new GraphStageLogic(shape) {

      setHandler(in, new InHandler {
        override def onPush(): Unit = {
          val input = grab(in)
        }
      })

      setHandler(retry, new InHandler {
        override def onPush(): Unit = {

        }
      })

      setHandler(out, new OutHandler {
        override def onPull(): Unit = pull(in)
      })
    }
  }
}
