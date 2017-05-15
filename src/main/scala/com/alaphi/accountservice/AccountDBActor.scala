package com.alaphi.accountservice

import scala.collection.Map
import scala.collection.immutable.HashMap
import akka.actor.{Actor, Props}

class AccountDBActor extends Actor {

  val ACC_NUM_BASE = 1000

  override def receive = store(HashMap[String, Account]())

  def store(storageMap: Map[String, Account]): Receive = {
    case ac: AccountCreation =>
      val newAccNum = (ACC_NUM_BASE + storageMap.size).toString
      val account = Account(newAccNum, ac.name, ac.balance)
      context.become(store(storageMap + ((newAccNum, account))))
      sender ! account

    case dt: DoMoneyTransfer =>
      sender ! Right(TransferSuccess(storageMap.get(dt.sourceAccNum).get, storageMap.get(dt.destAccNum).get, dt.transferAmount)) // TODO: carry out transfer here, return Either to cater for left or right response

    case ra: RemoveAccount => context.become(store(storageMap - ra.key))

    case ga: GetAccount    => sender ! storageMap.get(ga.key)

    case _ =>
  }

}

object AccountDBActor {
  def props = Props(new AccountDBActor)
}
