package org.reactivecouchbase.webstack.env

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.{ActorMaterializer, Materializer}
import com.typesafe.config.ConfigFactory
import org.reactivecouchbase.webstack.config.Configuration
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContext

sealed trait Mode
case object Dev extends Mode
case object Test extends Mode
case object Prod extends Mode

object Mode {
  def prod = Prod
  def test = Test
  def dev = Dev
  def valueOf(name: String): Option[Mode] = name match {
    case "Dev"  => Some(Dev)
    case "Test" => Some(Test)
    case "Prod" => Some(Prod)
    case "dev"  => Some(Dev)
    case "test" => Some(Test)
    case "prod" => Some(Prod)
    case _ => None
  }
}

object Env {
  private val DEFAULT = Configuration(ConfigFactory.load)
  private val APP_LOGGER: Logger = LoggerFactory.getLogger("application")

  private val system = ActorSystem.create("global-system", configuration.underlying.atPath("webstack.systems.global").withFallback(ConfigFactory.empty()))
  private val materializer = ActorMaterializer.create(system)
  private val executor = system.dispatcher
  // offered to the internals of actions
  private[webstack] val blockingSystem = ActorSystem.create("blocking-system", configuration.underlying.atPath("webstack.systems.blocking").withFallback(ConfigFactory.empty()))
  private[webstack] val blockingActorMaterializer = ActorMaterializer.create(blockingSystem)
  private[webstack] val blockingExecutor = blockingSystem.dispatcher

  // offered to the internals of ws
  private[webstack] val wsSystem = ActorSystem.create("ws-system", configuration.underlying.atPath("webstack.systems.ws").withFallback(ConfigFactory.empty()))
  // private[webstack] val wsClientActorMaterializer = ActorMaterializer.create(wsSystem)
  // private[webstack] val wsExecutor = wsSystem.dispatcher
  private[webstack] val wsHttp = Http.get(wsSystem)

  // offered to the internals of websockets
  private[websocket] val websocketSystem = ActorSystem.create("websocket-system", configuration.underlying.atPath("webstack.systems.websocket").withFallback(ConfigFactory.empty()))
  // private[websocket] val websocketActorMaterializer = ActorMaterializer.create(websocketSystem)
  // private[websocket] val websocketExecutor = websocketSystem.dispatcher
  private[websocket] val websocketHttp = Http.get(websocketSystem)

  Runtime.getRuntime.addShutdownHook(new Thread(new Runnable {
    override def run(): Unit = {
      system.terminate()
      blockingSystem.terminate()
      wsSystem.terminate()
      websocketSystem.terminate()
    }
  }))

  def logger: Logger = APP_LOGGER
  def configuration: Configuration = DEFAULT
  def globalActorSystem: ActorSystem = system
  def globalMaterializer: Materializer = materializer
  def globalExecutionContext: ExecutionContext = executor
  lazy val mode: Mode = Mode.valueOf(configuration.getString("app.mode").getOrElse("Prod")).getOrElse(Mode.prod)
}