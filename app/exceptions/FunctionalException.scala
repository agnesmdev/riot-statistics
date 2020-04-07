package exceptions

sealed class FunctionalException(m: String, c: Throwable) extends Exception(m, c)

case class NotFoundException(element: String) extends FunctionalException(s"$element does not exist", null)

case class MissingParameterException(parameter: String) extends FunctionalException(s"Missing parameter: $parameter", null)

case class InvalidJsonException(error: String) extends FunctionalException(s"Invalid json input, error: $error", null)

