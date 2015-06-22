package actors

import actors.Common._
import actors.ExchangeAccountant.{SaleAccepted, TotalExchange, Sale}
import akka.actor.{Actor, ActorLogging}

object ExchangeAccountant {
  case class Sale(id: Int, amountSale: Amount, amountBuy: Amount)
  case class TotalExchange(exchange: Exchange, totalSale: Amount, totalBuy: Amount)

  case class SaleAccepted(id: Int)
}

class ExchangeAccountant(exchange: Exchange) extends Actor with ActorLogging {

  var totalSale: Amount = 0
  var totalBuy: Amount = 0

  var expectedId: Int = 0

  override def receive: Receive = {
    case Sale(id, amountSale, amountBuy) => id match {
      case n if n > expectedId => log.debug("Ignoring sale with future id={}", n)
      case n if n < expectedId => {
        log.debug("Sale already processed: id={}", n)
        sender() ! SaleAccepted(n)
      }
      case n => {
        log.debug("Processing sale: id={}", n)

        totalSale += amountSale
        totalBuy += amountBuy

        log.debug("Total {}: sold={} bought={}", exchange, totalSale, totalBuy)

        context.system.eventStream.publish(TotalExchange(exchange, totalSale, totalBuy))

        sender() ! SaleAccepted(id)
        expectedId = expectedId + 1
      }
    }
  }

}

