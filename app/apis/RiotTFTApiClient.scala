package apis

import apis.ApiConstants.RIOT_TOKEN_HEADER
import com.google.inject.{ImplementedBy, Inject, Singleton}
import play.api.Configuration
import play.api.libs.ws.{WSClient, WSRequest}


@ImplementedBy(classOf[RiotTFTApiClientImpl])
trait RiotTFTApiClient {

  def getSummonerMatches(puuid: String)(regionHost: String): WSRequest

  def getMatch(matchId: String)(regionHost: String): WSRequest
}

@Singleton
class RiotTFTApiClientImpl @Inject()(ws: WSClient, configuration: Configuration) extends RiotTFTApiClient {

  private def apiHost(regionHost: String): String = configuration.get[String](regionHost)
  private val apiKey: String = configuration.get[String]("riot.api.key")

  private val maxCount: Int = 1000000000

  override def getSummonerMatches(puuid: String)(regionHost: String): WSRequest = {
    ws.url(s"${apiHost(regionHost)}/tft/match/v1/matches/by-puuid/$puuid/ids?count=$maxCount").withHttpHeaders(RIOT_TOKEN_HEADER -> apiKey)
  }

  override def getMatch(matchId: String)(regionHost: String): WSRequest = {
    ws.url(s"${apiHost(regionHost)}/tft/match/v1/matches/$matchId").withHttpHeaders(RIOT_TOKEN_HEADER -> apiKey)
  }
}
