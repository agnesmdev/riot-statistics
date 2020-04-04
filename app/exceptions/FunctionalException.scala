package exceptions

import java.util.UUID

sealed class FunctionalException(m: String, c: Throwable) extends Exception(m, c)

case class NotFoundException(id: UUID) extends FunctionalException(s"Element with id $id does not exist", null)
