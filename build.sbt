lazy val scala_2_13 = "2.13.2"

lazy val IntegrationTest = config("it") extend Test
concurrentRestrictions in Global += Tags.limit(Tags.Test, 1)

lazy val root = (project in file("."))
  .configs(IntegrationTest)
  .settings(
    Defaults.itSettings,
    name                := "queue",
    organization        := "io.mdcatapult.klein",
    scalaVersion        := scala_2_13,
    crossScalaVersions  := scala_2_13 :: Nil,
    useCoursier := false,
    scalacOptions ++= Seq(
      "-encoding", "utf-8",
      "-unchecked",
      "-deprecation",
      "-explaintypes",
      "-feature",
      "-Xlint",
      "-Xfatal-warnings",
    ),
    resolvers         ++= Seq(
      "MDC Nexus Releases" at "https://nexus.wopr.inf.mdc/repository/maven-releases/",
      "MDC Nexus Snapshots" at "https://nexus.wopr.inf.mdc/repository/maven-snapshots/"),
    credentials       += {
      sys.env.get("NEXUS_PASSWORD") match {
        case Some(p) =>
          Credentials("Sonatype Nexus Repository Manager", "nexus.wopr.inf.mdc", "gitlab", p)
        case None =>
          Credentials(Path.userHome / ".sbt" / ".credentials")
      }
    },
    dependencyOverrides += "org.scala-lang.modules" %% "scala-java8-compat" % "1.0.2",
    libraryDependencies ++= {
      val kleinUtilVersion = "1.2.4"

      val configVersion = "1.4.2"
      val playWsStandaloneVersion = "2.1.7"
      val akkaVersion = "2.8.1"
      val scalaTestVersion = "3.2.15"
      val scopedFixturesVersion = "2.0.0"

      Seq(
        "io.mdcatapult.klein" %% "util"                  % kleinUtilVersion,

        "org.scalatest" %% "scalatest"                   % scalaTestVersion % "it,test",
        "com.typesafe" % "config"                        % configVersion,
        "com.typesafe.play" %% "play-ahc-ws-standalone"  % playWsStandaloneVersion,
        "com.typesafe.play" %% "play-ws-standalone-json" % playWsStandaloneVersion,
        "com.typesafe.akka" %% "akka-actor"              % akkaVersion % "it,test",
        "com.typesafe.akka" %% "akka-slf4j"              % akkaVersion % "it,test",
        "com.typesafe.akka" %% "akka-stream"             % akkaVersion % "it,test",
        "com.typesafe.akka" %% "akka-testkit"            % akkaVersion % "it,test",
        "com.lightbend.akka" %% "akka-stream-alpakka-amqp" % "6.0.1"
      )
    }
  ).
  settings(
    publishSettings: _*
  )

lazy val publishSettings = Seq(
  publishTo := {
    if (isSnapshot.value)
      Some("MDC Maven Repo" at "https://nexus.wopr.inf.mdc/repository/maven-snapshots/")
    else
      Some("MDC Nexus" at "https://nexus.wopr.inf.mdc/repository/maven-releases/")
  },
  credentials += Credentials(Path.userHome / ".sbt" / ".credentials")
)
