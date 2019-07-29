import play.sbt.routes.RoutesKeys
import sbt.Keys.{libraryDependencies, organization}

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .configs(IntegrationTest)
  .settings(Defaults.itSettings)
  .settings(name := """bows-formula-one-api""",
    organization := "com.bowsformulaone",
    version := "1.0-SNAPSHOT",
    scalaVersion := "2.13.0",
    libraryDependencies += guice,
    libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3" % Test,
    libraryDependencies += "org.reactivemongo" %% "play2-reactivemongo" % "0.18.1-play27",
    libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.8",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.8" % Test,
    libraryDependencies += "org.mockito" % "mockito-all" % "1.10.19" % Test,
    RoutesKeys.routesImport += "models.CardId"
  )

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.bowsformulaone.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.bowsformulaone.binders._"
