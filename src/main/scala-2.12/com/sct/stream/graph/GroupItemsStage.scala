package com.sct.stream.graph

import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}

import scala.collection.mutable

trait SizeInfo { def size: Int }
case class GroupNotReached[A](acc: List[A]) extends Exception

/**
  * Parameter type should be a subclass of SizeInfo, used to provide
  * informations about the size of the original collection.
  * Main focus is that elements of the list that reach this stage
  * maintain the order.
  *
  * @param prev     the first element. Treat it like first foldLeft element
  * @param toGroup  high order function to check if object is part of group
  * @tparam A       an either representing the list that contains the
  *                 elements of the group or a GroupNotReached exception
  */
class GroupItemsStage[A <: SizeInfo](prev: A)(toGroup: (A, A) => Boolean)
    extends GraphStage[FlowShape[A, Either[Throwable, List[A]]]] {

  private[this] val in: Inlet[A]                            = Inlet[A]("GroupItem.in")
  private[this] val out: Outlet[Either[Throwable, List[A]]] = Outlet[Either[Throwable, List[A]]]("GroupItem.out")

  private[this] val acc: mutable.ListBuffer[A] = mutable.ListBuffer.empty[A]

  /**
    * Previous grabbed element
    */
  private[this] var previous: A = prev

  override val shape: FlowShape[A, Either[Throwable, List[A]]] = FlowShape.of(in, out)

  override def createLogic(attr: Attributes): GraphStageLogic = {
    new GraphStageLogic(shape) {
      setHandler(in, new InHandler {
        override def onPush(): Unit = {
          val next = grab(in)
          if (acc.isEmpty) {
            acc += next
            pull(in)
          } else {
            if (toGroup(previous, next)) {
              if (next.size == acc.size + 1) {
                push(out, Right((acc += next).toList))
                acc.clear()
              } else {
                acc += next
                pull(in)
              }
            } else {
              push(out, Left(GroupNotReached[A](acc.toList)))
              acc.clear()
              acc += next
            }
          }
          previous = next
        }
      })
      setHandler(out, new OutHandler { override def onPull(): Unit = pull(in) })
    }
  }
}
