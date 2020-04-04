package exceptions

sealed class TechnicalException(m: String, c: Throwable) extends Exception(m, c)

case class ApiException(e: Throwable) extends TechnicalException(s"Connection failed, error: ${e.getMessage}", e)
