package com.gameserver.double_card_game

import akka.actor.{Actor, ActorRef, ActorSystem, PoisonPill, Props}
import akka.http.scaladsl.model.ws.TextMessage
import com.gameserver.deck.{Card, Hand}
import com.gameserver.game._
import com.gameserver.player.{ActivePlayers, Player, PlayerStatus}
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext

class DoubleCardGamePlayerActor(player: Player, dealerActorRef: ActorRef)(implicit ec: ExecutionContext) extends Actor {

  val logger = LoggerFactory.getLogger(this.getClass)

  private var originalSender: Option[ActorRef] = None

  var handDrawn: Option[Hand] = None

  /**
    * States
    * Waiting for apponent
    * Playing
    * @return
    */

  // Waiting for apponent
  def receive: Receive = {
    case BeginGame =>
      logger.info(s"${player.id} - Beginning the game ...")
      player.sendMessage(s"You have been matched with an opponent. Beginning the game ...")
      context.become(playing)
  }

  // Playing state
  def playing: Receive = {
    case hand: Hand =>
      logger.debug(s"Got drawn card from dealer")
      handDrawn = Some(hand)
      player.sendMessage(s"Card drawn is ${hand.toString}")
      player.sendMessage(s"Please type your decision from: ${PlayerDecision.allS.mkString(",")}")

    case inputMessage: TextMessage => // Incoming message from player
      logger.debug(s"Got input from player ${player.id}")
      val playerDesisionString = inputMessage.getStrictText
      if(PlayerDecision.allS.contains(playerDesisionString)) {
        handDrawn.foreach(
          hand =>
            dealerActorRef ! DoubleCardGamePlayerDecision(player, hand, PlayerDecision.withName(playerDesisionString))
        )
      } else {
        val message =
          s"Invalid decision $playerDesisionString. Please choose one of ${PlayerDecision.allS.mkString(",")}"
        player.sendMessage(message)
      }

    case gameResult: GameResult =>

      // Sending game result to player
      gameResult.isWinner match {
        case None =>
          player.sendMessage("Game draw. Dealer will deal cards again ...")
          context.become(receive)
        case Some(true) => player.sendMessage("Congratulations !! You win ...")
        case Some(false) => player.sendMessage("Alas !! You loose ...")
      }

      // Awarding tokens
      player.grantToken(gameResult.prize)

    case EndGame =>
      logger.info(s"${player.id} - Ending the game ...")
      player.sendMessage("Ending the game...")
      player.sendChooseGameMessage
      player.status = PlayerStatus.IDLE
      player.gameActorRef = None
      player.gameBeingPlayed = None
      self ! PoisonPill
  }


}

object DoubleCardGamePlayer {

  def createActor(player: Player, dealerActorRef: ActorRef)
            (implicit ec: ExecutionContext, system: ActorSystem): ActorRef = {
    system.actorOf(Props(new DoubleCardGamePlayerActor(player, dealerActorRef)))
  }
}

