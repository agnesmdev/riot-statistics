package helpers

import play.api.Logger

trait LoggingHelper {

  val logger: Logger = Logger("lol.statistics")
}
