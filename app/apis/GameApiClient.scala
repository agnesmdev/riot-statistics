package apis

import com.google.inject.{ImplementedBy, Inject, Singleton}
import play.api.Configuration
import play.api.libs.ws.{WSClient, WSRequest}


@ImplementedBy(classOf[GameApiClientImpl])
trait GameApiClient {

  def getQueues: WSRequest

  def getSeasons: WSRequest
}

@Singleton
class GameApiClientImpl @Inject()(ws: WSClient, configuration: Configuration) extends GameApiClient {

  private val apiHost: String = configuration.get[String]("game.api.host")

  override def getQueues: WSRequest = {
    ws.url(s"$apiHost/queues.json")
  }

  override def getSeasons: WSRequest = {
    ws.url(s"$apiHost/seasons.json")
  }
}
