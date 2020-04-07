package models

sealed trait Region {

  def value: String

  def host: String

  override def toString: String = value
}

case object EuwRegion extends Region {
  override val value: String = "EUW"

  override val host: String = "riot.api.euw.host"
}

case object NaRegion extends Region {
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
