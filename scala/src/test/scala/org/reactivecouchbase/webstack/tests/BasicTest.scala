package org.reactivecouchbase.webstack.tests

import java.util.concurrent.TimeUnit

import akka.stream.scaladsl.Source
import org.reactivecouchbase.webstack.WebStackApp
import org.reactivecouchbase.webstack.actions.Action
import org.reactivecouchbase.webstack.result.Results._
import play.api.libs.json.Json

import scala.concurrent.duration.FiniteDuration

object Routes extends WebStackApp with App {

  GET   ->     "/hello"    ->     MyController.index
  GET   ->     "/stream"   ->     MyController.stream

  start
}

object MyController {

  def index = Action.sync { ctx =>
    Ok.text("Hello World!")
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
}
