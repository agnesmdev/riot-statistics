package modules

import com.mailjet.client.{ClientOptions, MailjetClient}
import play.api.inject.{Binding, Module}
import play.api.{Configuration, Environment}


class RiotModule extends Module {

  override def bindings(environment: Environment, configuration: Configuration): scala.collection.Seq[Binding[_]] = {
    Seq(
      bind[MailjetClient].toInstance(Mailjet.client(configuration))
    )
  }

  object Mailjet {
    def client(configuration: Configuration): MailjetClient = {
      val apiKey = configuration.get[String]("mailjet.api.key")
      val apiSecret = configuration.get[String]("mailjet.api.secret")
      val version = configuration.get[String]("mailjet.api.version")

      new MailjetClient(apiKey, apiSecret, new ClientOptions(version))
    }
  }

}
