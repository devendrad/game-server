package com.gameserver.player

import com.gameserver.game.Game.Game

import scala.collection.mutable.Queue
import scala.collection.mutable

object ActivePlayers {

  private val players: mutable.Set[Player] = mutable.Set.empty

  def getPlayerById(playerId: String): Option[Player] = players.find(_.id == playerId)

  def add(player: Player) = {
    players.add(player)
  }

  def remove(player: Player) = {
    players.remove(player)
  }

  def getPlayerToMatch(game: Game): Option[Player] = {
    players
      .filter(_.status == PlayerStatus.IDLE)
      .find(_.gameBeingPlayed.contains(game))
  }

  def getPlayerQueue(game: Game): Queue[Player] = {
    players
      .filter(_.gameBeingPlayed.contains(game))
      .to[collection.mutable.Queue]
  }

}