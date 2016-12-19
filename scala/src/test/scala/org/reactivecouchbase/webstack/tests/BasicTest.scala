package org.reactivecouchbase.webstack.tests

import java.util.concurrent.TimeUnit

import akka.stream.scaladsl.Source
import org.reactivecouchbase.webstack.{BootstrappedContext, ClassPathDirectory, WebStackApp}
import org.reactivecouchbase.webstack.actions.Action
import org.reactivecouchbase.webstack.env.Env
import org.reactivecouchbase.webstack.result.Results._
import org.reactivecouchbase.webstack.ws.WS
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import play.api.libs.json.{JsObject, Json}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.{Duration, FiniteDuration}

object RunnableRoutes extends WebStackApp with App {

  Get    ->    "/sse"   ->          MyController.stream
  Get    ->    "/sayhello" ->       MyController.index
  Get    ->    "/test" ->           MyController.text
  Get    ->    "/huge" ->           MyController.hugeText
  Get    ->    "/json" ->           MyController.json
  Get    ->    "/html" ->           MyController.html
  Get    ->    "/template" ->       MyController.template
  Get    ->    "/ws" ->             MyController.testWS
  Get    ->    "/ws2" ->            MyController.testWS2
  Get    ->    "/hello/{name}" ->   MyController.hello
  Post   ->    "/post" ->           MyController.testPost
  Assets ->    "/assets" ->         ClassPathDirectory("public")

  start()
}

object MyController {

  implicit val ec  = Env.globalExecutionContext
  implicit val mat = Env.globalMaterializer

  def index = Action.sync { ctx =>
    Ok.text("Hello World!\n")
  }

  def stream = Action.sync { ctx =>
    Ok.stream(
      Source.tick(FiniteDuration(0, TimeUnit.SECONDS), FiniteDuration(1, TimeUnit.SECONDS), "")
        .map(l => Json.obj(
          "time" ->System.currentTimeMillis(),
          "value" -> l
        )).map(Json.stringify).map(j => s"data: $j\n\n")
    ).as("text/event-stream")
  }

  def text = Action.sync { ctx =>
    Ok.text("Hello World!\n")
  }

  def hello = Action.sync { ctx =>
    Ok.text("Hello " + ctx.pathParam("name").getOrElse("Unknown") + "!\n")
  }

  def hugeText = Action.sync { ctx =>
    Ok.text(HUGE_TEXT + "\n")
  }

  def json = Action.sync { ctx =>
    Ok.json(Json.obj("message" -> "Hello World!"))
  }

  def html = Action.sync { ctx =>
    Ok.html("<h1>Hello World!</h1>")
  }

  def template = Action.sync { ctx =>
    Ok.template("hello", Map("name" -> ctx.queryParam("who").getOrElse("Mathieu")))
  }

  def testPost = Action.async { ctx =>
    ctx.body
      .map(body => body.json.as[JsObject])
      .map(payload => payload ++ Json.obj("processed_by" -> "SB"))
      .map(Ok.json)
  }

  def testWS = Action.async { ctx =>
    WS.host("http://freegeoip.net").withPath("/json/")
      .call()
      .flatMap(_.body)
      .map(r => Json.prettyPrint(r.json))
      .map(p => Ok.json(p))
  }

  def testWS2 = Action.async { ctx =>
    WS.host("http://freegeoip.net")
      .withPath("/json/")
      .withHeader("Sent-At", System.currentTimeMillis() + "")
      .call()
      .flatMap(_.body)
      .map(r => Json.prettyPrint(r.json))
      .map(p => Ok.json(p))
  }

  val HUGE_TEXT = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vestibulum rhoncus ultrices neque, nec consectetur ex molestie et. Integer dolor purus, laoreet vel condimentum vel, pulvinar at augue. Quisque tempor ac nisl vitae faucibus. Nunc placerat lacus dolor, nec finibus nibh semper eget. Nullam ac ipsum egestas, porttitor leo eget, suscipit risus. Donec sit amet est at erat pellentesque condimentum eu quis mauris. Aliquam tristique consectetur neque, a euismod magna mattis in. Nullam ac orci lectus. Interdum et malesuada fames ac ante ipsum primis in faucibus. Curabitur iaculis, mauris non tempus sagittis, eros nisl maximus quam, sed euismod sapien est id nisl. Nulla vitae enim dictum, tincidunt lorem nec, posuere arcu. Nulla tempus elit eu magna euismod maximus. Morbi varius nulla velit, eget pulvinar augue gravida eu.\n" + "Curabitur enim nisl, sollicitudin at odio laoreet, finibus gravida tellus. Nulla auctor urna magna, non egestas eros dignissim sollicitudin. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae; Nullam eget magna sit amet magna venenatis consequat vel vel lectus. Morbi fringilla pulvinar diam sed fermentum. Praesent ac tincidunt urna. Praesent in mi dolor. Curabitur posuere massa quis lectus fringilla, at congue ante faucibus. Mauris massa lacus, egestas quis consequat ac, pretium quis arcu. Fusce placerat vel massa eu blandit.\n" + "Curabitur fermentum, ante a tristique interdum, enim diam pulvinar urna, nec aliquet tellus lectus id lectus. Integer ullamcorper lacinia est vulputate pretium. In a dictum velit. In mattis justo sollicitudin iaculis iaculis. Quisque suscipit lorem vel felis accumsan, quis lobortis diam imperdiet. Nullam ornare metus massa, rutrum ullamcorper metus scelerisque a. Nullam finibus diam magna, et fringilla dui faucibus vel. Etiam semper libero sit amet ullamcorper consectetur. Curabitur velit ipsum, cursus sit amet justo eget, rhoncus congue enim. In elit ex, sodales vel odio non, ultricies egestas risus. Proin venenatis consectetur augue, et vestibulum leo dictum vel. Etiam id risus vitae dolor viverra blandit ut ac ante.\n" + "Quisque a nibh sem. Nulla facilisi. Ut gravida, dui et malesuada interdum, nunc arcu eleifend ligula, quis ornare tortor quam at ante. Vestibulum ac porta nibh, vitae imperdiet erat. Pellentesque nec lacus ex. Nullam sed hendrerit lacus. Curabitur varius sem sit amet tortor sollicitudin auctor. Donec eu feugiat enim, quis pellentesque urna. Morbi finibus fermentum varius. Aliquam quis efficitur nisi. Cras at tortor erat. Vestibulum interdum diam lacus, a lacinia mauris dapibus ut. Suspendisse potenti.\n" + "Vestibulum vel diam nec felis sodales porta nec sit amet eros. Quisque sit amet molestie risus. Pellentesque turpis ante, aliquam at urna vel, pulvinar fermentum massa. Proin posuere eu erat id condimentum. Nulla imperdiet erat a varius laoreet. Curabitur sollicitudin urna non commodo condimentum. Ut id ligula in ligula maximus pulvinar et id eros. Fusce et consequat orci. Maecenas leo sem, tristique quis justo nec, accumsan interdum quam. Nunc imperdiet scelerisque iaculis. Praesent sollicitudin purus et purus porttitor volutpat. Duis tincidunt, ipsum vel dignissim imperdiet, ligula nisi ultrices velit, at sodales felis urna at mi. Donec arcu ligula, pulvinar non posuere vel, accumsan eget lorem. Vivamus ac iaculis enim, ut rutrum felis. Praesent non ultrices nibh. Proin tristique, nibh id viverra varius, orci nisi faucibus turpis, quis suscipit sem nisi eu purus."
}

object SpecImplicits {
  implicit final class EnhancedFuture[A](future: Future[A]) {
    def await = Await.result(future, Duration(4, TimeUnit.SECONDS))
  }
}

object BasicTestSpecRoutes extends WebStackApp {

  Get    ->    "/sse"   ->          MyController.stream
  Get    ->    "/sayhello" ->       MyController.index
  Get    ->    "/test" ->           MyController.text
  Get    ->    "/huge" ->           MyController.hugeText
  Get    ->    "/json" ->           MyController.json
  Get    ->    "/html" ->           MyController.html
  Get    ->    "/template" ->       MyController.template
  Get    ->    "/ws" ->             MyController.testWS
  Get    ->    "/ws2" ->            MyController.testWS2
  Get    ->    "/hello/{name}" ->   MyController.hello
  Post   ->    "/post" ->           MyController.testPost
  Assets ->    "/assets" ->         ClassPathDirectory("public")

}

class BasicTestSpec extends FlatSpec with Matchers with BeforeAndAfterAll {

  import SpecImplicits._

  implicit val ec  = Env.globalExecutionContext
  implicit val mat = Env.globalMaterializer

  var server: BootstrappedContext = _

  val port = 7001

  override protected def beforeAll(): Unit = {
    server = BasicTestSpecRoutes.start(Some(port))
  }

  override protected def afterAll(): Unit = {
    server.stop
  }

  "Webstack" should "be able to respond with simple text result" in {
    val future = for {
      resp     <- WS.host("http://localhost", port).addPathSegment("sayhello").call()
      body     <- resp.body
    } yield (body.body, resp.status, resp.header("Content-Type").getOrElse("none"))
    val (body, status, contentType) = future.await
    assert(status == 200)
    assert(body == "Hello World!\n")
    assert(contentType == "text/plain")
  }

}
