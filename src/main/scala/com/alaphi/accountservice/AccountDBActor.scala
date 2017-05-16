package com.alaphi.accountservice

import scala.collection.Map
import scala.collection.immutable.HashMap
import akka.actor.{Actor, Props}

/**
  * Database Wrapper Actor
  *
  * Accounts are stored in immutable storageMap: Map[String, Account] which is a parameter to the receive handler named store
  *
  * When the storageMap is modified context.become is called to make store(changedMap) the new receive handler for incoming messages
  *
  * Akka Actor's mailbox ensures that each incoming messages are handled atomically, one at a time
  */
class AccountDBActor extends Actor {

  val ACC_NUM_BASE = 1000

  override def receive = store(HashMap[String, Account]())

  def store(storageMap: Map[String, Account]): Receive = {
    case ac: AccountCreation =>                                                                                           // Handles AccountCreation message
      val newAccNum = (ACC_NUM_BASE + storageMap.size).toString                                                           // Build account number as String so alpha numeric is possible
      val account = Account(newAccNum, ac.name, ac.balance)                                                               // create account to save in memory
      context.become(store(addAccountsToMap(storageMap, List(account))))                                                  // make store(changedMap) the new receive handler for incoming messages
      sender ! account                                                                                                    // Send created account to sender as ACK

    case dt: DoMoneyTransfer =>                                                                                           // Transfer operation is handled atomically
      val txfr = transfer(storageMap.get(dt.sourceAccNum), storageMap.get(dt.destAccNum), dt.transferAmount)              // Get src and dest accounts from memory and pass to transfer
      val txfrResult = txfr.toRight[TransferFailed](TransferFailed(dt.sourceAccNum, dt.destAccNum, dt.transferAmount))    // Convert Option[TransferSuccess] to Either, Right(TransferSuccess) or Left(TransferFailed)
      txfrResult foreach (r => context.become(store(addAccountsToMap(storageMap, List(r.sourceAccount, r.destAccount))))) // make store(changedMap) the new receive handler for incoming messages
      sender ! txfrResult                                                                                                 // Send Either[TransferFailed, TransferSuccess] to sender to signify transfer completed or failed

    case ra: RemoveAccount => context.become(store(storageMap - ra.key))                                                  // make store(changedMap) the new receive handler for incoming messages

    case ga: GetAccount    => sender ! storageMap.get(ga.key)

    case _ =>
  }

  def transfer(srcAcc: Option[Account], destAcc: Option[Account], amount: Int): Option[TransferSuccess] =                 // Returns Some(transferSuccess) or None for failed
    for {
      src <- srcAcc
      dest <- destAcc
      if (src.balance >= amount)
      srcAdjusted = src.copy(balance = src.balance - amount)
      destAdjusted = dest.copy(balance = dest.balance + amount)
    } yield TransferSuccess(srcAdjusted, destAdjusted, amount)

  def addAccountsToMap(storageMap: Map[String, Account], accounts: List[Account]) = storageMap ++ (accounts map (a => (a.number, a)))  // Convert List[Account] to List[(AccNum,Account)]

}

object AccountDBActor {
  def props = Props(new AccountDBActor)
}
