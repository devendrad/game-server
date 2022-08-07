package com.gameserver.player

import akka.actor.ActorRef
import akka.http.scaladsl.model.ws.TextMessage
import com.gameserver.game.Game
import com.gameserver.game.Game.Game
import com.gameserver.token.Token

case class Player(id: String, socketActorRef: ActorRef, name: String =  "default") {

  var status = PlayerStatus.IDLE

  var gameBeingPlayed: Option[Game] = None
  var gameActorRef: Option[ActorRef] = None

  val token = new Token

  def grantToken(tokenGrant: Long) = {
    token.add(tokenGrant)
    if (tokenGrant > 0L) {
      sendMessage(s"$tokenGrant tokens are awarded to you. You now have ${token.tokenValue} tokens.")
    } else if (tokenGrant < 0L) {
      sendMessage(s"${tokenGrant * -1} tokens are deducted from you. You now have ${token.tokenValue} tokens.")
    }
  }

  def sendMessage(message: String) = {
    socketActorRef ! TextMessage.apply(message)
  }

  def sendWelcomeMesssage = sendMessage(
    s"Welcome Player. You have been granted ${token.tokenValue} tokens. Your player Id is ${id}"
  )

  def sendChooseGameMessage = sendMessage(
    s"Choose a game. Available games are ${Game.allS.mkString(",")}"
  )
}
