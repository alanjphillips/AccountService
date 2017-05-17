package com.alaphi.accountservice

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{HttpEntity, MediaTypes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.util.ByteString
import com.alaphi.accountservice.AccountRoutesSpec._
import de.heikoseeberger.akkahttpcirce.CirceSupport._
import io.circe.Json
import io.circe.generic.auto._
import io.circe.parser.parse
import io.circe.syntax._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.Future

class AccountRoutesSpec extends WordSpec with Matchers with MockitoSugar with ScalatestRouteTest {
  val accountService = mock[AccountService]

  val accRoutes = AccountRoutes(accountService).routes

  when(accountService.create(createAccount)).thenReturn(Future.successful(srcAccount))
  when(accountService.transfer(srcAccount.accNumber, moneyTransfer)).thenReturn(Future.successful(Right(transferSuccess)))
  when(accountService.deposit(srcAccount.accNumber, deposit)).thenReturn(Future.successful(Right(depositSuccess)))
  when(accountService.read(srcAccount.accNumber)).thenReturn(Future.successful(Right(srcAccount)))
  when(accountService.readAll).thenReturn(Future.successful(List(destAccountAfter, srcAccount)))

  "AccountRoutes" should {

    "Create an Account" in {
      Post("/accounts", HttpEntity(MediaTypes.`application/json`, createAccountJson)) ~> accRoutes ~> check {
        status shouldBe OK
        responseAs[Account].asJson.noSpaces shouldBe accountJsonNoSpaces
      }
    }

    "Request fails due to unexpected RuntimeException" in {
      when(accountService.create(createAccount)).thenReturn(Future.failed(new RuntimeException("fail")))

      Post("/accounts", HttpEntity(MediaTypes.`application/json`, createAccountJson)) ~> accRoutes ~> check {
        status shouldBe InternalServerError
      }
    }

    "Transfer between 2 Accounts" in {
      Post("/accounts/1000/transfer", HttpEntity(MediaTypes.`application/json`, transferAccountJson)) ~> accRoutes ~> check {
        status shouldBe OK
        responseAs[TransferSuccess].asJson.noSpaces shouldBe transferSuccessJsonNoSpaces
      }
    }

    "Transfer between 2 Accounts fails due to insufficient funds" in {
      when(accountService.transfer(srcAccount.accNumber, moneyTransfer)).thenReturn(Future.successful(Left(transferFailed)))

      Post("/accounts/1000/transfer", HttpEntity(MediaTypes.`application/json`, transferAccountJson)) ~> accRoutes ~> check {
        status shouldBe BadRequest
        responseAs[TransferFailed].asJson.noSpaces shouldBe transferFailedJsonNoSpaces
      }
    }

    "Transfer between 2 Accounts fails due to account not found" in {
      when(accountService.transfer(srcAccount.accNumber, moneyTransfer)).thenReturn(Future.successful(Left(accountNotFound)))

      Post("/accounts/1000/transfer", HttpEntity(MediaTypes.`application/json`, transferAccountJson)) ~> accRoutes ~> check {
        status shouldBe NotFound
        responseAs[AccountNotFound].asJson.noSpaces shouldBe accountsNotFoundJsonNoSpaces
      }
    }

    "Deposit to an Account" in {
      Post("/accounts/1000/deposit", HttpEntity(MediaTypes.`application/json`, depositAccountJson)) ~> accRoutes ~> check {
        status shouldBe OK
        responseAs[DepositSuccess].asJson.noSpaces shouldBe depositSuccessJsonNoSpaces
      }
    }

    "Deposit to an Account fails due to account not found" in {
      when(accountService.deposit(srcAccount.accNumber, deposit)).thenReturn(Future.successful(Left(accountNotFound)))

      Post("/accounts/1000/deposit", HttpEntity(MediaTypes.`application/json`, depositAccountJson)) ~> accRoutes ~> check {
        status shouldBe NotFound
        responseAs[AccountNotFound].asJson.noSpaces shouldBe accountsNotFoundJsonNoSpaces
      }
    }

    "Get an Account" in {
      Get("/accounts/1000") ~> accRoutes ~> check {
        status shouldBe OK
        responseAs[Account].asJson.noSpaces shouldBe accountJsonNoSpaces
      }
    }

    "Get an Account fails due to account not found" in {
      when(accountService.read(srcAccount.accNumber)).thenReturn(Future.successful(Left(accountNotFound)))

      Get("/accounts/1000") ~> accRoutes ~> check {
        status shouldBe NotFound
        responseAs[AccountNotFound].asJson.noSpaces shouldBe accountsNotFoundJsonNoSpaces
      }
    }

    "Get all Accounts" in {
      Get("/accounts") ~> accRoutes ~> check {
        status shouldBe OK
        responseAs[List[Account]].asJson.noSpaces shouldBe allAccountsJsonNoSpaces
      }
    }

  }

}

object AccountRoutesSpec {
  val createAccount = AccountCreation("Joey", 25000)
  val srcAccount = Account("1000", "Joey", 25000)
  val accountNotFound = AccountNotFound("1000", "AccountNotFound")

  val moneyTransfer = MoneyTransfer("1001", 10000)
  val srcAccountAfter = Account("1000", "Joey", 15000)
  val destAccountAfter = Account("1001", "Junior", 10000)
  val transferSuccess = TransferSuccess(srcAccountAfter, destAccountAfter, 10000)
  val transferFailed = TransferFailed("1000", "1001", 10000, "TransferFailed")

  val deposit = Deposit(3000)
  val depositSuccess = DepositSuccess(srcAccount, 3000)

  val createAccountJson = ByteString(
    s"""
       |{
       |    "accHolderName":"Joey",
       |    "balance":25000
       |}
        """.stripMargin)

  val accountJson =
    s"""
       |{
       |    "accNumber":"1000",
       |    "accHolderName":"Joey",
       |    "balance":25000
       |}
        """.stripMargin

  val transferAccountJson = ByteString(
    s"""
       |{
       |  "destAccNum":"1001",
       |  "transferAmount":10000
       |}
        """.stripMargin)

  val transferSuccessJson =
    s"""
       |{
       |  "sourceAccount": {
       |    "accNumber": "1000",
       |    "accHolderName": "Joey",
       |    "balance": 15000
       |  },
       |  "destAccount": {
       |    "accNumber": "1001",
       |    "accHolderName": "Junior",
       |    "balance": 10000
       |  },
       |  "transferAmount": 10000
       |}
        """.stripMargin

  val depositAccountJson = ByteString(
    s"""
       |{
       |  "depositAmount":3000
       |}
        """.stripMargin)

  val depositSuccessJson =
    s"""
       |{
       |  "account": {
       |    "accNumber": "1000",
       |    "accHolderName": "Joey",
       |    "balance": 25000
       |  },
       |  "depositAmount": 3000
       |}
        """.stripMargin

  val allAccountsJson =
    s"""
       |[
       |  {
       |    "accNumber": "1001",
       |    "accHolderName": "Junior",
       |    "balance": 10000
       |  },
       |  {
       |    "accNumber": "1000",
       |    "accHolderName": "Joey",
       |    "balance": 25000
       |  }
       |]
        """.stripMargin

  val transferFailedJson =
    s"""
       |{
       |  "sourceAccNum": "1000",
       |  "destAccNum": "1001",
       |  "transferAmount": 10000,
       |  "description": "TransferFailed"
       |}
        """.stripMargin

  val accountNotFoundJson =
    s"""
       |{
       |  "accNumber": "1000",
       |  "description": "AccountNotFound"
       |}
        """.stripMargin

  val accountJsonNoSpaces = parse(accountJson).getOrElse(Json.Null).noSpaces
  val transferSuccessJsonNoSpaces = parse(transferSuccessJson).getOrElse(Json.Null).noSpaces
  val transferFailedJsonNoSpaces = parse(transferFailedJson).getOrElse(Json.Null).noSpaces
  val depositSuccessJsonNoSpaces = parse(depositSuccessJson).getOrElse(Json.Null).noSpaces
  val allAccountsJsonNoSpaces = parse(allAccountsJson).getOrElse(Json.Null).noSpaces
  val accountsNotFoundJsonNoSpaces = parse(accountNotFoundJson).getOrElse(Json.Null).noSpaces

}

