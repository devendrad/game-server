package com.gameserver.deck

sealed trait Suit

object Suit {
  case object Diamond extends Suit
  case object Spade extends Suit
  case object Club extends Suit
  case object Heart extends Suit
  val all = List(Diamond, Spade, Club, Heart)
}