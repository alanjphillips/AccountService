package com.alaphi.accountservice

import akka.actor.{ActorSystem, PoisonPill, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.duration._
import AccountDBActorSpec._

class AccountDBActorSpec
  extends TestKit(ActorSystem("AccountDBActorSpec"))
    with ImplicitSender
    with WordSpecLike
    with Matchers
    with ScalaFutures
    with BeforeAndAfterAll
    with MockitoSugar {

  "An AccountDBActor" should {
    "Respond with Account message representing created Account" in {
      within(500 millis) {
        val dbActorRef = system.actorOf(AccountDBActor.props)

        dbActorRef ! accountCreation
        expectMsg(account)

        dbActorRef ! PoisonPill
      }
    }

    "Respond with TransferSuccess message for DoMoneyTransfer" in {
      within(500 millis) {
        val dbActorRef = system.actorOf(AccountDBActor.props)

        dbActorRef ! accountCreation
        expectMsg(account)
        dbActorRef ! destAccountCreation
        expectMsg(destAccount)
        dbActorRef ! doMoneyTransfer
        expectMsg(Right(transferSuccess))

        dbActorRef ! PoisonPill
      }
    }

    "Respond with TransferFailed message for DoMoneyTransfer" in {
      within(500 millis) {
        val dbActorRef = system.actorOf(AccountDBActor.props)

        dbActorRef ! accountCreation
        expectMsg(account)
        dbActorRef ! destAccountCreation
        expectMsg(destAccount)
        dbActorRef ! doMoneyTransferBad
        expectMsg(Left(transferFailed))

        dbActorRef ! PoisonPill
      }
    }

    "Respond with AccountNotFound message for DoMoneyTransfer" in {
      within(500 millis) {
        val dbActorRef = system.actorOf(AccountDBActor.props)

        dbActorRef ! accountCreation
        expectMsg(account)
        dbActorRef ! doMoneyTransfer
        expectMsg(Left(accountNotFound))

        dbActorRef ! PoisonPill
      }
    }

    "Respond with DepositSuccess message" in {
      within(500 millis) {
        val dbActorRef = system.actorOf(AccountDBActor.props)

        dbActorRef ! accountCreation
        expectMsg(account)
        dbActorRef ! doDeposit
        expectMsg(Right(depositSuccess))

        dbActorRef ! PoisonPill
      }
    }

    "Respond with AccountNotFound message for DoDeposit" in {
      within(500 millis) {
        val dbActorRef = system.actorOf(AccountDBActor.props)

        dbActorRef ! doDeposit
        expectMsg(Left(accountNotFoundDep))

        dbActorRef ! PoisonPill
      }
    }

    "Respond with Account message representing Account number in GetAccount" in {
      within(500 millis) {
        val dbActorRef = system.actorOf(AccountDBActor.props)

        dbActorRef ! accountCreation
        expectMsg(account)
        dbActorRef ! getAccount
        expectMsg(Right(account))

        dbActorRef ! PoisonPill
      }
    }

    "Respond with AccountNotFound message for Account number in GetAccount" in {
      within(500 millis) {
        val dbActorRef = system.actorOf(AccountDBActor.props)

        dbActorRef ! getAccount
        expectMsg(Left(accountNotFoundDep))

        dbActorRef ! PoisonPill
      }
    }

    "Respond with List of all Accounts message when 2 Accounts are already created" in {
      within(500 millis) {
        val dbActorRef = system.actorOf(AccountDBActor.props)

        dbActorRef ! accountCreation
        expectMsg(account)
        dbActorRef ! destAccountCreation
        expectMsg(destAccount)
        dbActorRef ! GetAllAccounts
        expectMsg(List(destAccount, account))

        dbActorRef ! PoisonPill
      }
    }

    "Respond with empty List of Accounts message when no Accounts are already created" in {
      within(500 millis) {
        val dbActorRef = system.actorOf(AccountDBActor.props)

        dbActorRef ! GetAllAccounts
        expectMsg(Nil)

        dbActorRef ! PoisonPill
      }
    }

    "Ignore unsupported message" in {
      within(500 millis) {
        val dbActorRef = system.actorOf(AccountDBActor.props)

        dbActorRef ! "NotSupported"
        expectNoMsg()

        dbActorRef ! PoisonPill
      }
    }
  }


  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

}

object AccountDBActorSpec {
  val accountCreation = AccountCreation("bob", 200)
  val destAccountCreation = AccountCreation("ann", 0)

  val account = Account("1000", "bob", 200)
  val accountDeducted = Account("1000", "bob", 150)

  val destAccount = Account("1001", "ann", 0)
  val destAccountAdded = Account("1001", "ann", 50)

  val moneyTransfer = MoneyTransfer("1002", 50)
  val doMoneyTransfer =  DoMoneyTransfer("1000", "1001", 50)
  val doMoneyTransferBad =  DoMoneyTransfer("1000", "1001", 300)
  val transferSuccess = TransferSuccess(accountDeducted, destAccountAdded, 50)
  val transferFailed = TransferFailed("1000", "1001", 300, "Not enough funds available in account number: 1000")

  val deposit = Deposit(75)
  val doDeposit = DoDeposit("1000", 75)
  val depositAccountAdded = Account("1000", "bob", 275)
  val depositSuccess = DepositSuccess(depositAccountAdded, 75)

  val getAccount = GetAccount("1000")
  val accountNotFound = AccountNotFound("1001", "Account Number doesn't exist: 1001")
  val accountNotFoundDep = AccountNotFound("1000", "Account Number doesn't exist: 1000")
}