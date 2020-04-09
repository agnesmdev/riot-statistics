package exceptions

sealed class TechnicalException(m: String, c: Throwable) extends Exception(m, c)

case class ApiException(status: Int, body: String) extends TechnicalException(s"status: $status, body: $body", null) {
  lazy val canBeRetried: Boolean = status >= 500
}

case class TooManyRequestsException(message: String) extends TechnicalException(message, null)

case class ServiceUnavailableException(message: String) extends TechnicalException(message, null)

case class ConnectionException(e: Throwable) extends TechnicalException(s"Connection failed, error: ${e.getMessage}", e)