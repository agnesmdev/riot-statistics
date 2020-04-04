package filters

import java.util.UUID

import akka.stream.Materializer
import helpers.LoggingHelper
import javax.inject.Inject
import org.slf4j.MDC
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}


class LoggingFilter @Inject()(implicit val mat: Materializer, ec: ExecutionContext) extends Filter with LoggingHelper {

  private val excludedPaths = Seq("/docs", "/assets", "/swagger")
  private val correlationIdHeader = "X-Correlation-ID"
  private val correlationIdParameter = "correlationId"

  def apply(nextFilter: RequestHeader => Future[Result])(requestHeader: RequestHeader): Future[Result] = {
    if (excludedPaths.exists(p => requestHeader.uri.startsWith(p))) {
      logger.trace(s"Processing ${requestHeader.method} ${requestHeader.uri}")
      nextFilter(requestHeader)
    } else {
      val correlationId = requestHeader.headers.get(correlationIdHeader).getOrElse(computeCorrelationId)
      logger.info(s"Processing ${requestHeader.method} ${requestHeader.uri}")

      MDC.put(correlationIdParameter, correlationId)
      nextFilter(requestHeader).map { result =>
        result.withHeaders(correlationIdHeader -> correlationId)
      }
    }
  }

  private def computeCorrelationId: String = UUID.randomUUID().toString.replaceAll("-", "")
}