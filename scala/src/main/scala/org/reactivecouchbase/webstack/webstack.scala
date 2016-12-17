package org.reactivecouchbase.webstack

import akka.http.scaladsl.model.{HttpMethod, HttpMethods}
import com.google.common.base.Throwables
import io.undertow.Handlers._
import io.undertow.server.handlers.resource.ClassPathResourceManager
import io.undertow.server.{HttpHandler, HttpServerExchange}
import io.undertow.util.HttpString
import io.undertow.{Handlers, Undertow}
import org.reactivecouchbase.webstack.actions.{Action, ReactiveActionHandler}
import org.reactivecouchbase.webstack.env.Env
import org.reflections.Reflections
import org.slf4j.LoggerFactory
import play.api.libs.json.Json

import scala.collection.JavaConversions._

case class BootstrappedContext(undertow: Undertow, app: WebStackApp) {
  def stopApp {
    try {
      app.beforeStop
      undertow.stop
      app.afterStop
    } catch {
      case e: Exception => e.printStackTrace
    }
  }
}

case class RootRoute(app: WebStackApp, method: HttpMethod) {
  def ->(template: String) = TemplateRoute(app, method, template)
}

case class TemplateRoute(app: WebStackApp, method: HttpMethod, template: String) {
  def ->(action: => Action) = app.route(method, template, action)
}

class WebStackApp {

  private[webstack] val routingHandler = Handlers.routing()

  def route(method: HttpMethod, url: String, action: => Action) {
    routingHandler.add(method.name, url, ReactiveActionHandler(action))
  }

  // def r(method: HttpMethod, url: String, action: => Action) = route(method, url, action)
  // def $(method: HttpMethod, url: String, action: => Action) = route(method, url, action)
  // def $(method: HttpMethod, url: String, action: => Action) = route(method, url, action)
  // def r(method: HttpMethod, url: String, action: WebSocketActionSupplier) = route(method, url, action)
  // def route(method: HttpMethod, url: String, action: WebSocketActionSupplier) {
  //   routingHandler.add(method.name, url, Handlers.websocket(new ReactiveWebSocketHandler(action)))
  // }
  // def defineRoutes {}

  def beforeStart {}

  def afterStart {}

  def beforeStop {}

  def afterStop {}

  def start: BootstrappedContext = WebStack.startWebStackApp(this)

  val CONNECT = RootRoute(this, HttpMethods.CONNECT)
  val DELETE  = RootRoute(this, HttpMethods.DELETE )
  val GET     = RootRoute(this, HttpMethods.GET    )
  val HEAD    = RootRoute(this, HttpMethods.HEAD   )
  val OPTIONS = RootRoute(this, HttpMethods.OPTIONS)
  val PATCH   = RootRoute(this, HttpMethods.PATCH  )
  val POST    = RootRoute(this, HttpMethods.POST   )
  val PUT     = RootRoute(this, HttpMethods.PUT    )
  val TRACE   = RootRoute(this, HttpMethods.TRACE  )
}

object WebStack extends App {

  def main(args: String*) {
    Env.logger.trace("Scanning classpath looking for WebStackApp implementations")
    new Reflections("").getSubTypesOf(classOf[WebStackApp]).headOption.map { serverClazz =>
      try {
        Env.logger.info(s"Found WebStackApp class: ${serverClazz.getName}")
        val context = serverClazz.newInstance()
        startWebStackApp(context)
      } catch {
        case e: Exception => throw Throwables.propagate(e)
      }
    }
  }

  private[webstack] def startWebStackApp(webstackApp: WebStackApp): BootstrappedContext = {
    Env.logger.trace("Starting WebStackApp")
    val port = Env.configuration.getInt("webstack.port").getOrElse(9000)
    val host = Env.configuration.getString("webstack.host").getOrElse("0.0.0.0")
    // webstackApp.defineRoutes
    val handler = webstackApp.routingHandler.setInvalidMethodHandler(new HttpHandler {
      override def handleRequest(ex: HttpServerExchange): Unit = {
        ex.setStatusCode(400)
        ex.getResponseHeaders.put(HttpString.tryFromString("Content-Type"), "application/json")
        ex.getResponseSender.send(Json.stringify(Json.obj(
          "error" -> s"Invalid Method ${ex.getRequestMethod} on uri ${ex.getRequestURI}"
        )))
      }
    }).setFallbackHandler(path.addPrefixPath("/assets", resource(new ClassPathResourceManager(classOf[WebStackApp].getClassLoader, "public"))))
    Env.logger.trace("Starting Undertow")
    val server = Undertow
      .builder()
      .addHttpListener(port, host)
      .setHandler(handler)
      .build()
    webstackApp.beforeStart
    server.start()
    webstackApp.afterStart
    Env.logger.trace("Undertow started")
    Env.logger.info("Running WebStack on http://" + host + ":" + port)
    val bootstrapedContext = BootstrappedContext(server, webstackApp)
    Env.logger.trace("Registering shutdown hook")
    Runtime.getRuntime.addShutdownHook(new Thread(new Runnable {
      override def run(): Unit = bootstrapedContext.stopApp
    }))
    Env.logger.trace("Init done")
    bootstrapedContext
  }
}
