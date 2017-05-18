package com.alaphi.accountservice

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import org.scalatest.mockito.MockitoSugar
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import AccountServiceSpec._

class AccountServiceSpec
  extends TestKit(ActorSystem("AccountServiceSpec"))
    with WordSpecLike
    with Matchers
    with ScalaFutures
    with BeforeAndAfterAll
    with MockitoSugar {

  val dbProbe = TestProbe()
  val accountService = AccountService(dbProbe.ref)

  "AccountService" should {

    "Create an Account" in {
      val accFut = accountService.create(accountCreation)
      dbProbe.expectMsg(0 millis, accountCreation)
      dbProbe.reply(account)

      whenReady(accFut)(
        result => result shouldBe account
      )
    }

    "Perform a transfer from one account to another account" in {
      val transferFut = accountService.transfer("1001", moneyTransfer)
      dbProbe.expectMsg(0 millis, doMoneyTransfer)
      dbProbe.reply(Right(transferSuccess))

      whenReady(transferFut)(
        result => result.right.get shouldBe transferSuccess
      )
    }

    "Fail to transfer due to insufficient funds in source account" in {
      val transferFut = accountService.transfer("1001", moneyTransfer)
      dbProbe.expectMsg(0 millis, doMoneyTransfer)
      dbProbe.reply(Left(transferFailed))

      whenReady(transferFut)(
        result => result.left.get shouldBe transferFailed
      )
    }

    "Fail to transfer due to account not found" in {
      val transferFut = accountService.transfer("1001", moneyTransfer)
      dbProbe.expectMsg(0 millis, doMoneyTransfer)
      dbProbe.reply(Left(accountNotFound))

      whenReady(transferFut)(
        result => result.left.get shouldBe accountNotFound
      )
    }

    "Perform a deposit to account" in {
      val depositFut = accountService.deposit("1001", deposit)
      dbProbe.expectMsg(0 millis, doDeposit)
      dbProbe.reply(Right(depositSuccess))

      whenReady(depositFut)(
        result => result.right.get shouldBe depositSuccess
      )
    }

    "Fail to deposit to account due to account not found" in {
      val depositFut = accountService.deposit("1001", deposit)
      dbProbe.expectMsg(0 millis, doDeposit)
      dbProbe.reply(Left(accountNotFound))

      whenReady(depositFut)(
        result => result.left.get shouldBe accountNotFound
      )
    }

    "Read an Account" in {
      val accFut = accountService.read("1001")
      dbProbe.expectMsg(0 millis, getAccount)
      dbProbe.reply(Right(account))

      whenReady(accFut)(
        result => result.right.get shouldBe account
      )
    }

    "Fail to read an Account due to account not found" in {
      val accFut = accountService.read("1001")
      dbProbe.expectMsg(0 millis, getAccount)
      dbProbe.reply(Left(accountNotFound))

      whenReady(accFut)(
        result => result.left.get shouldBe accountNotFound
      )
    }

    "Read all Accounts" in {
      val accsFut = accountService.readAll
      dbProbe.expectMsg(0 millis, GetAllAccounts)
      dbProbe.reply(List(account, destAccount))

      whenReady(accsFut)(
        result => result shouldBe List(account, destAccount)
      )
    }

  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

}

object AccountServiceSpec {
  val accountCreation = AccountCreation("bob", 200)
  val account = Account("1001", "bob", 200)

  val destAccount = Account("1002", "ann", 0)
  val moneyTransfer = MoneyTransfer("1002", 50)
  val doMoneyTransfer =  DoMoneyTransfer("1001", "1002", 50)
  val transferSuccess = TransferSuccess(account, destAccount, 50)
  val transferFailed = TransferFailed("1001", "1002", 333, "InsufficientFunds")

  val deposit = Deposit(75)
  val doDeposit = DoDeposit("1001", 75)
  val depositSuccess = DepositSuccess(account, 75)

  val getAccount = GetAccount("1001")
  val accountNotFound = AccountNotFound("1001", "AccountNotFound")
}
