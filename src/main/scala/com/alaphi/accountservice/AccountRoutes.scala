package com.alaphi.accountservice

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatchers.Segment
import akka.http.scaladsl.server.StandardRoute
import de.heikoseeberger.akkahttpcirce.CirceSupport._
import io.circe.generic.auto._

import scala.util.{Failure, Success, Try}

/**
  * AccountRoutes: REST Interface for Accounts
  *
  * @param accountService
  */
class AccountRoutes(accountService: AccountService) {

  val successHandler: PartialFunction[Try[_], StandardRoute] = {
    case Success(acc: Account)                => complete(OK -> acc)
    case Success(accs: List[Account])         => complete(OK -> accs)
    case Success(Right(acc: Account))         => complete(OK -> acc)
    case Success(Right(trs: TransferSuccess)) => complete(OK -> trs)
    case Success(Right(dep: DepositSuccess))  => complete(OK -> dep)
  }

  val operationalFailureHandler: PartialFunction[Try[_], StandardRoute] = {
    case Success(Left(trf: TransferFailed))  => complete(BadRequest -> trf)
    case Success(Left(anf: AccountNotFound)) => complete(NotFound -> anf)
  }

  val failureHandler: PartialFunction[Try[_], StandardRoute] = {
    case Failure(f) => complete(BadRequest -> f)
  }

  val responseHandler: PartialFunction[Try[_], StandardRoute] = successHandler orElse operationalFailureHandler orElse failureHandler

  val routes = {
    path("accounts") {
      post {
        entity(as[AccountCreation]) { accountCreation =>
          onComplete(accountService.create(accountCreation))(responseHandler)
        }
      }
    } ~
    path("accounts" / Segment / "transfer") { accountNumber =>
      post {
        entity(as[MoneyTransfer]) { moneyTransfer =>
          onComplete(accountService.transfer(accountNumber, moneyTransfer))(responseHandler)
        }
      }
    } ~
    path("accounts" / Segment / "deposit") { accountNumber =>
      post {
        entity(as[Deposit]) { deposit =>
          onComplete(accountService.deposit(accountNumber, deposit))(responseHandler)
        }
      }
    } ~
    path("accounts" / Segment) { accountNumber =>
      get(onComplete(accountService.read(accountNumber))(responseHandler))
    } ~
    path("accounts") {
      get(onComplete(accountService.readAll)(responseHandler))
    }

  }

}

object AccountRoutes {
  def apply(accountService: AccountService): AccountRoutes = new AccountRoutes(accountService)
}

