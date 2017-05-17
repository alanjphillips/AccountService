package com.alaphi.accountservice

import scala.collection.Map
import scala.collection.immutable.HashMap
import akka.actor.{Actor, Props}

/**
  * AccountDBActor: Database Wrapper Actor
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
    case ac: AccountCreation =>                                                                                                    // Handles AccountCreation message
      val newAccNum = (ACC_NUM_BASE + storageMap.size).toString                                                                    // Build account number as String so alpha numeric is possible
      val account = Account(newAccNum, ac.name, ac.balance)                                                                        // create account to save in memory
      context.become(store(addAccountsToMap(storageMap, List(account))))                                                           // make store(changedMap) the new receive handler for incoming messages
      sender ! account                                                                                                             // Send created account to sender as ACK

    case dt: DoMoneyTransfer =>                                                                                                    // Transfer operation is handled atomically
      val src = getAccount(storageMap, dt.sourceAccNum)
      val dest = getAccount(storageMap, dt.destAccNum)
      val txfrResult = transfer(src, dest, dt.transferAmount)                                                                      // Get src and dest accounts from memory and pass to transfer
      txfrResult foreach (r => context.become(store(addAccountsToMap(storageMap, List(r.sourceAccount, r.destAccount)))))          // make store(changedMap) the new receive handler for incoming messages
      sender ! txfrResult                                                                                                          // Send Either[TransferFailed, TransferSuccess] to sender to signify transfer completed or failed

    case ga: GetAccount    => sender ! storageMap.get(ga.key)

    case _ =>
  }

  def transfer(srcAcc: Either[AccountNotFound, Account], destAcc: Either[AccountNotFound, Account], amount: Int): Either[AccountError, TransferSuccess] =                           // Returns Some(transferSuccess) or None for failed
    for {
      src <- srcAcc
      dest <- destAcc
      res <- adjust(src, dest, amount)
    } yield res

  def adjust(src: Account, dest: Account, amount: Int): Either[TransferFailed, TransferSuccess] = {
    if (src.balance >= amount) {
      Right(
        TransferSuccess(
          src.copy(balance = src.balance - amount),
          dest.copy(balance = dest.balance + amount),
          amount
        )
      )
    } else Left(TransferFailed(src.number, dest.number, amount, "Not enough funds"))
  }

  def addAccountsToMap(storageMap: Map[String, Account], accounts: List[Account]) =
    storageMap ++ (accounts map (a => (a.number, a)))                                                                                // Convert List[Account] to List[(AccNum,Account)]

  def getAccount(storageMap: Map[String, Account], accountNumber: String): Either[AccountNotFound, Account] = {
    storageMap.get(accountNumber).toRight[AccountNotFound](AccountNotFound(accountNumber))
  }
}

object AccountDBActor {
  def props = Props(new AccountDBActor)
}
