import Settings._

import scala.concurrent.duration._
val dbDriver   = "com.mysql.jdbc.Driver"
val dbName     = "wallets"
val dbUser     = "wallets"
val dbPassword = "passwd"
val dbPort     = 3310
val dbUrl      = s"jdbc:mysql://localhost:$dbPort/$dbName?useSSL=false"

lazy val infrastructure: Project = (project in file("infrastructure"))
  .settings(commonSettings)
  .settings(
    name := "wallets-infrastructure",
    libraryDependencies ++= Seq(
      ScalaDddBase.version,
      TypesafeConfig.version,
      Commons.codec,
      Shapeless.version,
      PureConfig.pureConfig,
      Monix.version,
      //      SeasarUtil.v0_0_1,
      //      Passay.latest,
      //      Apache.commonsLang,
      T3hnar.bCrypt
    )
  )
  .disablePlugins(WixMySQLPlugin)

lazy val domain: Project = (project in file("domain"))
  .settings(commonSettings)
  .settings(
    name := "wallets-domain",
    libraryDependencies ++= Seq(
    )
  )
  .dependsOn(infrastructure)

val `contract-usecase` = (project in file("contract-usecase"))
  .settings(commonSettings)
  .settings(
    name := "wallets-contract-usecase",
    libraryDependencies ++= Seq(
    )
  )
  .dependsOn(domain)

val `contract-interface` = (project in file("contract-interface"))
  .settings(commonSettings)
  .settings(
    name := "wallets-contract-interface",
    libraryDependencies ++= Seq(
    ) ++ Swagger.all
  )
  .dependsOn(`contract-usecase`)

lazy val usecase: Project = (project in file("usecase"))
  .settings(commonSettings)
  .settings(
    name := "wallets-usecase",
    libraryDependencies ++= Seq(
      Akka.stream,
      Akka.testkit % Test
    )
  )
  .dependsOn(`contract-usecase`, `contract-interface`, domain, infrastructure)
  .disablePlugins(WixMySQLPlugin)

lazy val interface: Project = (project in file("interface"))
  .settings(commonSettings)
  .settings(
    name := "wallets-interface",
    libraryDependencies ++= Seq(
      ScalaDddBase.slick,
      ScalaDddBase.dynamodb,
      ReactiveDynamoDB.test % Test,
      Circe.core,
      Circe.parser,
      Circe.generic,
      Slick.slick,
      Slick.hikaricp,
      AWSSDK.kms,
      Aws.encryptionSdkJava,
      AWSSDK.s3,
      AWSSDK.sqs,
      SQS.elasticmqRestSqs % Test,
      Akka.testkit         % Test,
      Akka.slf4j,
      Akka.http,
      Akka.httpTestKit % Test,
      Akka.stream,
      Akka.persistence,
      Heikoseeberger.`akka-http-crice`,
      Logback.classic % Test,
      ScalaTestPlusDB.version % Test,
      ChatWork.AkkaGuard.http,
      Hayasshi.router,
      CORS.`akka-http`
    ) ,
    parallelExecution in Test := false,
    // JDBCのドライバークラス名を指定します(必須)
    driverClassName in generator := dbDriver,
    // JDBCの接続URLを指定します(必須)
    jdbcUrl in generator := dbUrl,
    // JDBCの接続ユーザ名を指定します(必須)
    jdbcUser in generator := dbUser,
    // JDBCの接続ユーザのパスワードを指定します(必須)
    jdbcPassword in generator := dbPassword,
    // カラム型名をどのクラスにマッピングするかを決める関数を記述します(必須)
    propertyTypeNameMapper in generator := {
      case "INTEGER" | "TINYINT"             => "Int"
      case "BIGINT"                          => "Long"
      case "BIGINT UNSIGNED"                 => "Long"
      case "VARCHAR" | "TEXT"                => "String"
      case "BOOLEAN" | "BIT"                 => "Boolean"
      case "DATE" | "TIMESTAMP" | "DATETIME" => "java.time.ZonedDateTime"
      case "DECIMAL"                         => "BigDecimal"
      case "ENUM"                            => "String"
    },
    tableNameFilter in generator := { tableName: String =>
      (tableName.toUpperCase != "SCHEMA_VERSION") && !tableName.toUpperCase.endsWith("ID_SEQUENCE_NUMBER")
    },
    outputDirectoryMapper in generator := {
      case s if s.endsWith("Spec") => (sourceDirectory in Test).value
      case s =>
        new java.io.File((scalaSource in Compile).value, "/com/github/j5ik2o/wallets/adaptor/storage/dao/jdbc")
    },
    // モデル名に対してどのテンプレートを利用するか指定できます。
    templateNameMapper in generator := {
      case className if className.endsWith("Spec") => "template_spec.ftl"
      case _                                       => "template.ftl"
    },
    compile in Compile := ((compile in Compile) dependsOn (generateAll in generator)).value,
    generateAll in generator := Def
      .taskDyn {
        val ga = (generateAll in generator).value
        Def
          .task {
            (wixMySQLStop in flyway).value
          }
          .map(_ => ga)
      }
      .dependsOn(flywayMigrate in flyway)
      .value
  )
  .dependsOn(`contract-interface`, usecase, infrastructure, flyway % "test->test")
//.disablePlugins(WixMySQLPlugin)

lazy val flyway: Project = (project in file("tools/flyway"))
  .settings(
    name := "wallets-flyway"
  )
  .settings(commonSettings)
  .settings(
    wixMySQLVersion := com.wix.mysql.distribution.Version.v5_6_21,
    wixMySQLUserName := Some(dbUser),
    wixMySQLPassword := Some(dbPassword),
    wixMySQLSchemaName := dbName,
    wixMySQLPort := Some(dbPort),
    wixMySQLDownloadPath := Some(sys.env("HOME") + "/.wixMySQL/downloads"),
    //wixMySQLTempPath := Some(sys.env("HOME") + "/.wixMySQL/work"),
    wixMySQLTimeout := Some(2 minutes),
    flywayDriver := dbDriver,
    flywayUrl := dbUrl,
    flywayUser := dbUser,
    flywayPassword := dbPassword,
    flywaySchemas := Seq(dbName),
    flywayLocations := Seq(
      s"filesystem:${baseDirectory.value}/src/test/resources/2019-01-23/",
      s"filesystem:${baseDirectory.value}/src/test/resources/2019-01-23/test",
    ),
    flywayPlaceholderReplacement := true,
    flywayPlaceholders := Map(
      "engineName"                 -> "MEMORY",
      "idSequenceNumberEngineName" -> "MyISAM"
    ),
    parallelExecution in Test := false,
    flywayMigrate := (flywayMigrate dependsOn wixMySQLStart).value
  )

lazy val `local-mysql` = (project in file("tools/local-mysql"))
  .settings(commonSettings)
  .settings(
    name := "wallets-local-mysql",
    wixMySQLVersion := com.wix.mysql.distribution.Version.v5_6_21,
    wixMySQLUserName := Some(dbUser),
    wixMySQLPassword := Some(dbPassword),
    wixMySQLSchemaName := dbName,
    wixMySQLPort := Some(3306),
    wixMySQLDownloadPath := Some(sys.env("HOME") + "/.wixMySQL/downloads"),
    wixMySQLTimeout := Some((30 seconds) * sys.env.getOrElse("SBT_TEST_TIME_FACTOR", "1").toDouble),
    flywayDriver := dbDriver,
    flywayUrl := s"jdbc:mysql://localhost:3306/$dbName?useSSL=false",
    flywayUser := dbUser,
    flywayPassword := dbPassword,
    flywaySchemas := Seq(dbName),
    flywayLocations := Seq(
      s"filesystem:${(baseDirectory in flyway).value}/src/test/resources/2019-01-23/",
      s"filesystem:${(baseDirectory in flyway).value}/src/test/resources/2019-01-23/test",
      s"filesystem:${baseDirectory.value}/src/main/resources/2019-01-23/dummy"
    ),
    flywayPlaceholderReplacement := true,
    flywayPlaceholders := Map(
      "engineName" -> "InnoDB",
      "idSequenceNumberEngineName" -> "MyISAM"
    ),
    run := (flywayMigrate dependsOn wixMySQLStart).value
  )

lazy val `api-server` = (project in file("api-server"))
  .enablePlugins(AshScriptPlugin, JavaAgent)
  .settings(commonSettings)
  .settings(
    name := "wallets-api-server",
    libraryDependencies ++= Seq(
      Logback.classic
    ) ++ Akka.managementAll,
    fork in run := true,
    javaAgents += "org.aspectj" % "aspectjweaver" % "1.8.13",
    javaOptions in Universal += "-Dorg.aspectj.tracing.factory=default",
    javaOptions in run ++= Seq(
      s"-Dcom.sun.management.jmxremote.port=${sys.env.getOrElse("JMX_PORT", "8999")}",
      "-Dcom.sun.management.jmxremote.authenticate=false",
      "-Dcom.sun.management.jmxremote.ssl=false",
      "-Dcom.sun.management.jmxremote.local.only=false",
      "-Dcom.sun.management.jmxremote"
    ),
    javaOptions in Universal ++= Seq(
      "-Dcom.sun.management.jmxremote",
      "-Dcom.sun.management.jmxremote.local.only=true",
      "-Dcom.sun.management.jmxremote.authenticate=false"
    )
  )
  .settings(dockerCommonSettings)
  .dependsOn(`interface`, `domain`)

lazy val root =
  (project in file("."))
    .aggregate(interface, usecase, domain)
    .disablePlugins(WixMySQLPlugin)