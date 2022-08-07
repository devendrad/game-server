package com.gameserver.deck

import scala.collection.mutable.Queue
import scala.util.Random

case class Card(number: Int, suit: Suit){
  override def toString: String = s"Card($number of $suit)"
}

case class Hand(cards: List[Card])

class Deck {

  var cards: Queue[Card] = Random.shuffle(
    for {
      suit <- Suit.all
      number <- 1 to 13
    } yield Card(number, suit)).to[collection.mutable.Queue]

  def get = cards

  def takeOut(n: Int): Hand = {
    Hand(1.to(n).map(x => cards.dequeue()).toList)
  }

  def take1(): Option[Card] = takeOut(1).cards.headOption

  private val rank: List[Int] = List(1) ++ (13 to 2).toList

  def compare(inputCard1: Card, inputCard2: Card): Option[Card] = {
    if(inputCard1.number > inputCard2.number) Some(inputCard1)
    else if (inputCard1.number < inputCard2.number) Some(inputCard2)
    else None
  }
}