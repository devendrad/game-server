package com.gameserver.double_card_game

import akka.actor.{Actor, ActorRef, ActorSystem, PoisonPill, Props}
import com.gameserver.deck.{Card, Deck, Hand}
import com.gameserver.game.PlayerDecision.PlayerDecision
import com.gameserver.game._
import com.gameserver.player.Player
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext

case class DoubleCardGamePlayerDecision(player: Player, hand: Hand, decision: PlayerDecision)

class DoubleCardGameDealerActor(deck: Deck, player1: Player, player2: Player)(implicit ec: ExecutionContext) extends Actor {

  val logger = LoggerFactory.getLogger(this.getClass)

  private var originalSender: Option[ActorRef] = None

  val noOfPlayers = 2

  var playerDecisionsMap: Map[Player, DoubleCardGamePlayerDecision] = Map.empty

  override def receive: Receive = {
    case DealCards => // Draw cards and send to players
      logger.debug(s"Got message to begin game between players ${player1.id} & ${player2.id}")

      playerDecisionsMap = Map.empty

      player1.gameActorRef.foreach(_ ! BeginGame)
      player2.gameActorRef.foreach(_ ! BeginGame)

      // drawing cards annd sending to players
      player1.gameActorRef.foreach(_ ! deck.takeOut(2))
      player2.gameActorRef.foreach(_ ! deck.takeOut(2))

    case playerDecision: DoubleCardGamePlayerDecision =>

      playerDecisionsMap = playerDecisionsMap + (playerDecision.player -> playerDecision)

      if(playerDecisionsMap.keys.size == noOfPlayers){

        val (player1Result, player2Result) = (playerDecisionsMap(player1).decision, playerDecisionsMap(player2).decision) match {
          case (PlayerDecision.fold, PlayerDecision.fold) => // each losses 2 token
            (GameResult(isWinner = Some(false), -2), GameResult(isWinner = Some(false), -2))

          case (PlayerDecision.play, PlayerDecision.fold) => // player1 wins 5 tokens and player2 losses 5 tokens
            (GameResult(isWinner = Some(true), 5), GameResult(isWinner = Some(false), -5))

          case (PlayerDecision.fold, PlayerDecision.play) => // player2 wins 5 tokens and player1 losses 5 tokens
            (GameResult(isWinner = Some(false), -5), GameResult(isWinner = Some(true), 5))

          case (PlayerDecision.play, PlayerDecision.play) => // comparision of cards

            // Calculating high card for first level comparison
            val player1HighCard = getHighestRankCard(player1)
            val player2HighCard = getHighestRankCard(player2)

            compare(player1HighCard, player2HighCard) match {
              case (GameResult(None, _), GameResult(None, _)) => // Tie in comparison of high cards. Hence comparig low cards
                val player1LowCard = playerDecisionsMap(player1).hand.cards.diff(List(player1HighCard)).head
                val player2LowCard = playerDecisionsMap(player2).hand.cards.diff(List(player2HighCard)).head
                compare(player1LowCard, player2LowCard)
              case highCardResults => highCardResults
            }
        }

        player1.gameActorRef.foreach(_ ! player1Result)
        player2.gameActorRef.foreach(_ ! player2Result)

        if(player1Result.isWinner.isEmpty && player2Result.isWinner.isEmpty)
          self ! DealCards
        else{
          List(player1, player2).foreach(_.gameActorRef.foreach(_ ! EndGame))
          self ! PoisonPill
        }
      }

  }

  def getHighestRankCard(player: Player): Card = {
    val cards = playerDecisionsMap(player).hand.cards
    deck.compare(cards.head, cards.tail.head).getOrElse(cards.head)
  }

  def compare(player1Card: Card, player2Card: Card): (GameResult, GameResult) = {
    deck.compare(player1Card, player2Card) match {
      case None => // Draw
        (GameResult(isWinner = None, 0), GameResult(isWinner = None, 0))
      case Some(`player1Card`) => // player1 wins 20 tokens and player2 losses 20 tokens
        (GameResult(isWinner = Some(true), 20), GameResult(isWinner = Some(false), -20))
      case Some(`player2Card`) => // player1 wins 20 tokens and player2 losses 20 tokens
        (GameResult(isWinner = Some(false), -20), GameResult(isWinner = Some(true), 20))
    }
  }
}

object DoubleCardGameDealer {

  def createActor(deck: Deck, p1: Player, p2: Player)
            (implicit ec: ExecutionContext, system: ActorSystem): ActorRef = {
    system.actorOf(Props(new DoubleCardGameDealerActor(deck, p1, p2)))
  }
}
