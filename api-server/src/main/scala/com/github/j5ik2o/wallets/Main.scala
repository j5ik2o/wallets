package com.github.j5ik2o.wallets

import akka.actor.ActorSystem
import akka.cluster.Cluster
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.complete
import akka.management.AkkaManagement
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory

object Main extends App {

  lazy val config = ConfigFactory.load()
  implicit val system = ActorSystem("wallets-api-server")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher
  implicit val cluster = Cluster(system)

  AkkaManagement(system).start()
  ClusterBootstrap(system).start()

  Http().bindAndHandle(complete(config.getString("application.api.hello-message")), config.getString("application.api.host"), config.getInt("application.api.port"))
}
