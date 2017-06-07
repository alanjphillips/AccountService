package com.alaphi.accountservice

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import scala.io.StdIn

object Boot extends App {

  implicit val system = ActorSystem("AccountService")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val db = system.actorOf(AccountDBActor.props)

  val client = system.actorOf(ClientActor.props(db))

  client ! StartSending


//  val accountService = AccountService(db)
//
//  val routes = AccountRoutes(accountService).routes
//
//  val bindingFuture = Http().bindAndHandle(routes, "0.0.0.0", 8081)
//
//  println(s"Account Service is running. Press RETURN to terminate")
//
//  StdIn.readLine()
//
//  bindingFuture.flatMap(_.unbind()).onComplete(_ => system.terminate())

}
