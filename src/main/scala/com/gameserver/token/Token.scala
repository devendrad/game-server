package com.gameserver.token

class Token {

  var tokenValue: Long = 1000L

  def get = tokenValue

  def add(addendum: Long) = {
    tokenValue = tokenValue + addendum
    tokenValue
  }
}
