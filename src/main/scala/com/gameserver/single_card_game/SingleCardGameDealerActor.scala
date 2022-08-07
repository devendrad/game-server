package com.gameserver.single_card_game

import akka.actor.{Actor, ActorRef, ActorSystem, PoisonPill, Props}
import com.gameserver.deck.{Card, Deck}
import com.gameserver.game.PlayerDecision.PlayerDecision
import com.gameserver.game._
import com.gameserver.player.Player
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext

case class SingleCardGamePlayerDecision(player: Player, card: Card, decision: PlayerDecision)

class SingleCardGameDealerActor(deck: Deck, player1: Player, player2: Player)(implicit ec: ExecutionContext) extends Actor {

  val logger = LoggerFactory.getLogger(this.getClass)

  private var originalSender: Option[ActorRef] = None

  val noOfPlayers = 2

  var playerDecisionsMap: Map[Player, SingleCardGamePlayerDecision] = Map.empty

  override def receive: Receive = {
    case DealCards => // Draw cards and send to players
      logger.debug(s"Got message to begin game between players ${player1.id} & ${player2.id}")

      playerDecisionsMap = Map.empty

      player1.gameActorRef.foreach(_ ! BeginGame)
      player2.gameActorRef.foreach(_ ! BeginGame)

      // drawing cards annd sending to players
      deck.take1().foreach(card => player1.gameActorRef.foreach(_ ! card))
      deck.take1().foreach(card => player2.gameActorRef.foreach(_ ! card))

    case playerDecision: SingleCardGamePlayerDecision =>
      playerDecisionsMap = playerDecisionsMap + (playerDecision.player -> playerDecision)

      if(playerDecisionsMap.keys.size == noOfPlayers){

        val (player1Result, player2Result) = (playerDecisionsMap(player1).decision, playerDecisionsMap(player2).decision) match {
          case (PlayerDecision.fold, PlayerDecision.fold) => // each loses 1 token
            (GameResult(isWinner = Some(false), -1), GameResult(isWinner = Some(false), -1))

          case (PlayerDecision.play, PlayerDecision.fold) => // player1 wins 3 tokens and player2 losses 3 tokens
            (GameResult(isWinner = Some(true), 3), GameResult(isWinner = Some(false), -3))

          case (PlayerDecision.fold, PlayerDecision.play) => // player2 wins 3 tokens and player1 losses 3 tokens
            (GameResult(isWinner = Some(false), -3), GameResult(isWinner = Some(true), 3))

          case (PlayerDecision.play, PlayerDecision.play) => // comparision of cards

            val player1Card = playerDecisionsMap(player1).card
            val player2Card = playerDecisionsMap(player2).card

            deck.compare(player1Card, player2Card) match {
              case None => // Draw
                (GameResult(isWinner = None, 0), GameResult(isWinner = None, 0))
              case Some(`player1Card`) => // player1 wins 10 tokens and player2 losses 10 tokens
                (GameResult(isWinner = Some(true), 10), GameResult(isWinner = Some(false), -10))
              case Some(`player2Card`) => // player1 wins 10 tokens and player2 losses 10 tokens
                (GameResult(isWinner = Some(false), -10), GameResult(isWinner = Some(true), 10))
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
}

object SingleCardGameDealer {

  def createActor(deck: Deck, p1: Player, p2: Player)
            (implicit ec: ExecutionContext, system: ActorSystem): ActorRef = {
    system.actorOf(Props(new SingleCardGameDealerActor(deck, p1, p2)))
  }
}
