package com.alaphi.accountservice

import akka.actor.{Actor, ActorRef, PoisonPill, Props}
import ClientActor._

case object StartSending

class ClientActor(db: ActorRef) extends Actor {

  var responseCount = 0
  var startTime = 0L
  var endTime = 0L

  val numTransfers = 10000000
  val reportInterval = 1000000

  override def receive = {
    case StartSending     =>
      db ! accountCreation
      db ! destAccountCreation
      startTime = System.currentTimeMillis()
      for (i <- 0L until numTransfers) {
        db ! doMoneyTransfer
      }

    case acc: Account          =>
      println(s"Created account: $acc")

    case ts: Either[AccountError, TransferSuccess]  =>
      responseCount += 1
      if (ts.isRight && responseCount % reportInterval == 0)
        println(s"Success Transfer count: $responseCount , ${ts}")
      if (responseCount == numTransfers) {
        endTime = System.currentTimeMillis()
        val timeTakenInSeconds = (endTime - startTime) / 1000
        println(s"timeTakenInSeconds: $timeTakenInSeconds")
        val msgPerSec = responseCount / timeTakenInSeconds
        println(s"Msgs/Sec: $msgPerSec")
        self ! PoisonPill
      }
  }


}

object ClientActor {
  def props(db: ActorRef) = Props(new ClientActor(db))

  val accountCreation = AccountCreation("bob", 10000000)
  val destAccountCreation = AccountCreation("ann", 0)

  val doMoneyTransfer =  DoMoneyTransfer("1000", "1001", 1)
}