package com.alaphi.accountservice

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatchers.Segment
import akka.http.scaladsl.server.StandardRoute
import de.heikoseeberger.akkahttpcirce.CirceSupport._
import io.circe.generic.auto._
import scala.util.{Failure, Success}

class AccountRoutes(accountService: AccountService) {

  val successHandler: PartialFunction[Any, StandardRoute] = {
    case Success(acc: Account)                => complete(OK -> acc)
    case Success(Right(trs: TransferSuccess)) => complete(OK -> trs)
  }

  val operationalFailureHandler: PartialFunction[Any, StandardRoute] = {
    case Success(Left(trf: TransferFailed)) => complete(BadRequest -> trf)
  }

  val failureHandler: PartialFunction[Any, StandardRoute] = {
    case Failure(f) => complete(BadRequest -> f)
  }


  val routes = {
    path("accounts") {
      post {
        entity(as[AccountCreation]) { accountCreation =>
          onComplete(accountService.create(accountCreation))(successHandler orElse operationalFailureHandler orElse failureHandler)
        }
      }
    } ~
    path("accounts" / Segment) { accountNumber =>
      get(onComplete(accountService.read(accountNumber))(successHandler orElse operationalFailureHandler orElse failureHandler))
    } ~
    path("accounts" / Segment / "transfer") { accountNumber =>
      post {
        entity(as[MoneyTransfer]) { moneyTransfer =>
          onComplete(accountService.transfer(accountNumber, moneyTransfer))(successHandler orElse operationalFailureHandler orElse failureHandler)
        }
      }
    }
  }

}

object AccountRoutes {
  def apply(accountService: AccountService): AccountRoutes = new AccountRoutes(accountService)
}

