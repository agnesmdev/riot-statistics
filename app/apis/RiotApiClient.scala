package apis

import apis.ApiConstants.RIOT_TOKEN_HEADER
import com.google.inject.{ImplementedBy, Inject, Singleton}
import play.api.Configuration
import play.api.libs.ws.{WSClient, WSRequest}


@ImplementedBy(classOf[RiotApiClientImpl])
trait RiotApiClient {

  def getSummoner(name: String)(regionHost: String): WSRequest

  def getSummonerMatches(accountId: String, beginIndex: Int)(regionHost: String): WSRequest

  def getMatch(matchId: Long)(regionHost: String): WSRequest
}

@Singleton
class RiotApiClientImpl @Inject()(ws: WSClient, configuration: Configuration) extends RiotApiClient {

  private def apiHost(regionHost: String): String = configuration.get[String](regionHost)
  private val apiKey: String = configuration.get[String]("riot.api.key")

  override def getSummoner(name: String)(regionHost: String): WSRequest = {
    ws.url(s"${apiHost(regionHost)}/lol/summoner/v4/summoners/by-name/$name").withHttpHeaders(RIOT_TOKEN_HEADER -> apiKey)
  }

  override def getSummonerMatches(accountId: String, beginIndex: Int)(regionHost: String): WSRequest = {
    ws.url(s"${apiHost(regionHost)}/lol/match/v4/matchlists/by-account/$accountId?beginIndex=$beginIndex").withHttpHeaders(RIOT_TOKEN_HEADER -> apiKey)
  }

  override def getMatch(matchId: Long)(regionHost: String): WSRequest = {
    ws.url(s"${apiHost(regionHost)}/lol/match/v4/matches/$matchId").withHttpHeaders(RIOT_TOKEN_HEADER -> apiKey)
  }
}
