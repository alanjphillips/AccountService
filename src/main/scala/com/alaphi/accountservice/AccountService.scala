package com.alaphi.accountservice

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/**
  * AccountService: Performs Asks to DB Actor, mapping Actor response to Future[_]
  *
  * @param db
  * @param ec
  */
class AccountService(db: ActorRef)(implicit ec: ExecutionContext) {

  implicit val timeout = Timeout(5 seconds)

  def create(accountCreation: AccountCreation): Future[Account] = (db ? accountCreation).mapTo[Account]

  def transfer(accountNumber: String, transfer: MoneyTransfer): Future[Either[AccountError, TransferSuccess]] = {
    val doTransfer = DoMoneyTransfer(accountNumber, transfer.destAccNum, transfer.transferAmount)
    (db ? doTransfer).mapTo[Either[AccountError, TransferSuccess]]
  }

  def read(accountNumber: String): Future[Either[AccountError, Account]] = (db ? GetAccount(accountNumber)).mapTo[Either[AccountError, Account]]

  def readAll: Future[List[Account]] = (db ? GetAllAccounts).mapTo[List[Account]]

}

object AccountService {
  def apply(db: ActorRef)(implicit ec: ExecutionContext): AccountService = new AccountService(db)
}