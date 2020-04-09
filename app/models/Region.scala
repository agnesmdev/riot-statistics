package models

sealed trait GlobalRegion {

  def globalValue: String

  def globalHost: String
}

sealed trait EuropeRegion extends GlobalRegion {
  override val globalValue: String = "EUROPE"

  override val globalHost: String = "riot.api.europe.host"
}

sealed trait AmericaRegion extends GlobalRegion {
  override val globalValue: String = "AMERICAS"

  override val globalHost: String = "riot.api.americas.host"
}

sealed trait Region extends GlobalRegion {

  def value: String

  def host: String

  override def toString: String = value
}

case object EuwRegion extends Region with EuropeRegion {
  override val value: String = "EUW"

  override val host: String = "riot.api.euw.host"
}

case object NaRegion extends Region with AmericaRegion {
  override val value: String = "NA"

  override val host: String = "riot.api.na.host"
}

object Region {
  def parse(value: String): Option[Region] = value match {
    case EuwRegion.value => Some(EuwRegion)
    case NaRegion.value => Some(NaRegion)
    case _ => None
  }
}
