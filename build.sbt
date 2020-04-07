name := "riot-statistics"
normalizedName := "RiotStatistics"
organization := "dev.agnesm"

version := "1.0.0"
scalaVersion := "2.12.10"

swaggerDomainNameSpaces := Seq("models")

lazy val root = (project in file("."))
  .enablePlugins(PlayScala, BuildInfoPlugin, SwaggerPlugin, SbtTwirl)
  .settings(
    buildInfoKeys := Seq[BuildInfoKey](name, normalizedName, version, scalaVersion, sbtVersion)
  )

resolvers += Resolver.bintrayRepo("tmacedo", "maven")

libraryDependencies ++= {
  Seq(
    guice,
    ws,
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "com.mailjet" % "mailjet-client" % "4.2.1",
    "com.typesafe.akka" %% "akka-actor" % "2.6.1",
    "uk.gov.hmrc" %% "emailaddress" % "2.1.0",
    "net.logstash.logback" % "logstash-logback-encoder" % "6.3",
    "org.webjars" % "swagger-ui" % "2.2.0",
    "org.mockito" % "mockito-core" % "3.3.0" % Test,
    "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test
  )
}