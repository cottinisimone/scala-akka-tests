package com.sct

import shapeless.{:+:, CNil, Coproduct, Inl, Inr}

object SomeCoproductTypeErasure extends App {

  type LawfulInterceptionPayload = Boolean :+: String :+: CNil

  println(matchSomePayload(Some(Coproduct[LawfulInterceptionPayload](true))) == 1)
  println(matchSomePayload(Some(Coproduct[LawfulInterceptionPayload]("ciao"))) == 2)
  println(matchSomePayload(None) == 4)

  def matchSomePayload(payload: Option[LawfulInterceptionPayload]): Int = payload match {
    case Some(Inl(_)) => 1
    case Some(Inr(Inl(_))) => 2
    case Some(Inr(Inr(_))) => 3
    case None => 4
  }
}
