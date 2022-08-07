package com.gameserver.player

object PlayerStatus extends Enumeration {
  type PlayerStatus = Value

  val IDLE = Value("IDLE")
  val PLAYING = Value("PLAYING")

  val IDLEs = IDLE.toString
  val PLAYINGs = PLAYING.toString
}
