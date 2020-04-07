package apis

import com.google.inject.{ImplementedBy, Inject, Singleton}
import play.api.Configuration
import play.api.libs.ws.{WSClient, WSRequest}


@ImplementedBy(classOf[DragonApiClientImpl])
trait DragonApiClient {

  def getChampions: WSRequest
}

@Singleton
class DragonApiClientImpl @Inject()(ws: WSClient, configuration: Configuration) extends DragonApiClient {

  private val apiHost: String = configuration.get[String]("dragon.api.host")

  override def getChampions: WSRequest = {
    ws.url(s"$apiHost/champions.json")
  }
}
