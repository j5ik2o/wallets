import com.amazonaws.services.s3.model.Region
import com.chatwork.sbt.aws.core.SbtAwsCorePlugin.autoImport._
import com.chatwork.sbt.aws.s3.SbtAwsS3Plugin.autoImport._
import com.chatwork.sbt.aws.s3.resolver.SbtAwsS3ResolverPlugin.autoImport._
import com.lucidchart.sbt.scalafmt.ScalafmtCorePlugin.autoImport._
import com.typesafe.sbt.SbtNativePackager.autoImport._
import com.typesafe.sbt.packager.archetypes.scripts.BashStartScriptPlugin.autoImport._
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport._
import sbt.Keys._
import sbt._

object Settings {
  lazy val commonSettings = Seq(
    organization := "com.github.j5ik2o",
    scalaVersion := "2.12.8",
    testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest,
      "-F",
      sys.env.getOrElse("SBT_TEST_TIME_FACTOR", "1")),
    scalacOptions ++= Seq(
      "-feature",
      "-deprecation",
      "-unchecked",
      "-encoding",
      "UTF-8",
      "-Xfatal-warnings",
      "-language:_",
      // Warn if an argument list is modified to match the receiver
      "-Ywarn-adapted-args",
      // Warn when dead code is identified.
      "-Ywarn-dead-code",
      // Warn about inaccessible types in method signatures.
      "-Ywarn-inaccessible",
      // Warn when a type argument is inferred to be `Any`.
      "-Ywarn-infer-any",
      // Warn when non-nullary `def f()' overrides nullary `def f'
      "-Ywarn-nullary-override",
      // Warn when nullary methods return Unit.
      "-Ywarn-nullary-unit",
      // Warn when numerics are widened.
      "-Ywarn-numeric-widen",
      // Warn when imports are unused.
      "-Ywarn-unused-import"
    ),
    resolvers ++= Seq(
      "Sonatype OSS Snapshot Repository" at "https://oss.sonatype.org/content/repositories/snapshots/",
      "Sonatype OSS Release Repository" at "https://oss.sonatype.org/content/repositories/releases/",
      Resolver.bintrayRepo("tanukkii007", "maven"),
      Resolver.bintrayRepo("everpeace", "maven"),
      Resolver.bintrayRepo("segence", "maven-oss-releases"),
      "DynamoDB Local Repository" at "https://s3-us-west-2.amazonaws.com/dynamodb-local/release"
    ),
    libraryDependencies ++= Seq(
      MySQLConnectorJava.version,
      ScalaTest.version       % Test,
      ScalaCheck.scalaCheck   % Test,
      ScalaMock.version       % Test,
      ScalaTestPlusDB.version % Test,
      Cats.version,
      Airframe.di,
      Enumeratum.version,
      Scala.java8Compat
    ) ++ Kamon.all,
    updateOptions := updateOptions.value.withCachedResolution(true),
    credentialProfileName in aws := scala.util.Properties.propOrNone("aws.profile").orElse(Some("maven@cwtech")),
    s3Region in aws := Region.AP_Tokyo,
    parallelExecution in Test := false,
    javaOptions in (Test, run) ++= Seq("-Xms4g", "-Xmx4g", "-Xss10M", "-XX:+CMSClassUnloadingEnabled"),
    scalafmtOnCompile in Compile := true,
    scalafmtTestOnCompile in Compile := true
  )

  lazy val dockerCommonSettings = Seq(
    dockerBaseImage := "adoptopenjdk/openjdk8:x86_64-alpine-jdk8u191-b12",
    maintainer in Docker := "Junichi Kato <j5ik2o@gmail.com>",
    packageName in Docker := s"j5ik2o/${name.value}",
    dockerUpdateLatest := true,
    bashScriptExtraDefines ++= Seq(
      "addJava -Xms${JVM_HEAP_MIN:-1024m}",
      "addJava -Xmx${JVM_HEAP_MAX:-1024m}",
      "addJava -XX:MaxMetaspaceSize=${JVM_META_MAX:-512M}",
      "addJava ${JVM_GC_OPTIONS:--XX:+UseG1GC}"
    )
  )

}