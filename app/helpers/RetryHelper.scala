package helpers

import akka.actor.Scheduler
import akka.pattern.after
import exceptions.{ApiException, TooManyRequestsException}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

trait RetryHelper extends LoggingHelper {

  def maxRetries: Int

  def unavailableDelay: FiniteDuration

  def tooMuchRequestsDelay: FiniteDuration

  def retry[A](fun: () => Future[A], retries: Int = maxRetries)(implicit ec: ExecutionContext, s: Scheduler): Future[A] = {
    fun().recoverWith {
      case _: TooManyRequestsException if retries > 0 =>
        logger.warn(s"Too many requests, retrying in ${tooMuchRequestsDelay.toSeconds} seconds")
        after(tooMuchRequestsDelay, s)(retry(fun, retries - 1))
      case e: ApiException if e.canBeRetried && retries > 0 =>
        logger.warn(s"Unavailable service, retrying in ${unavailableDelay.toSeconds} seconds")
        after(unavailableDelay, s)(retry(fun, retries - 1))
    }
  }
}
