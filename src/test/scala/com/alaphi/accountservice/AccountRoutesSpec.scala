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

  when(accountService.create(createAccount)).thenReturn(Future.successful(account))

  "AccountRoutes" should {

    "Create an Account" in {
      Post("/accounts", HttpEntity(MediaTypes.`application/json`, createAccountJson)) ~> accRoutes ~> check {
        status shouldBe OK
        responseAs[Account].asJson.noSpaces shouldBe accountJsonNoSpaces
      }
    }

  }

}

object AccountRoutesSpec {
  val createAccount = AccountCreation("Joey")
  val account = Account("123abc", "Joey", 0)

  val createAccountJson = ByteString(
    s"""
       |{
       |    "name":"Joey",
       |    "balance":0
       |}
        """.stripMargin)

  val accountJson =
    s"""
       |{
       |    "number":"123abc",
       |    "name":"Joey",
       |    "balance":0
       |}
        """.stripMargin

  val accountJsonNoSpaces = parse(accountJson).getOrElse(Json.Null).noSpaces
}

