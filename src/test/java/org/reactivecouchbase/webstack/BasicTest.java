package org.reactivecouchbase.webstack;

import akka.http.javadsl.model.HttpMethods;
import javaslang.collection.List;
import javaslang.collection.Map;
import javaslang.collection.Traversable;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.reactivecouchbase.common.Duration;
import org.reactivecouchbase.concurrent.Await;
import org.reactivecouchbase.concurrent.Future;
import org.reactivecouchbase.functional.Tuple;
import org.reactivecouchbase.json.JsObject;
import org.reactivecouchbase.json.JsValue;
import org.reactivecouchbase.json.Json;
import org.reactivecouchbase.webstrack.BootstrappedContext;
import org.reactivecouchbase.webstrack.libs.ws.WS;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

public class BasicTest {

    private static final Duration MAX_AWAIT = Duration.parse("4s");

    private static BootstrappedContext server;

    @BeforeClass
    public static void setUp() {
        server = TestApplication.run();
    }

    @AfterClass
    public static void tearDown() {
        server.stopApp();
    }

    @Test
    public void testTextResult() {
        Future<Tuple<String, Map<String, List<String>>>> fuBody = WS.host("http://localhost:9000")
            .withPath("/sayhello").call()
            .flatMap(r -> r.body().map(b ->
                Tuple.of(
                    b.body(),
                    r.headers()
                )
            ));
        Tuple<String, Map<String, List<String>>> body = Await.result(fuBody, MAX_AWAIT);
        Assert.assertEquals("Hello World!\n", body._1);
        Assert.assertEquals("text/plain", body._2.get("Content-Type").flatMap(Traversable::headOption).getOrElse("none"));
    }


    @Test
    public void testPathParamResult() throws Exception {
        Future<Tuple<String, Map<String, List<String>>>> fuBody = WS.host("http://localhost:9000")
                .withPath("/hello/Mathieu").call()
                .flatMap(r -> r.body().map(b ->
                        Tuple.of(
                                b.body(),
                                r.headers()
                        )
                ));
        Tuple<String, Map<String, List<String>>> body = Await.result(fuBody, MAX_AWAIT);
        Assert.assertEquals("Hello Mathieu!\n", body._1);
        Assert.assertEquals("text/plain", body._2.get("Content-Type").flatMap(Traversable::headOption).getOrElse("none"));
    }

    @Test
    public void testHugeTextResult() throws Exception {
        Future<Tuple<String, Map<String, List<String>>>> fuBody = WS.host("http://localhost:9000")
                .withPath("/huge")
                .withHeader("Api-Key", "12345")
                .call()
                .flatMap(r -> r.body().map(b ->
                        Tuple.of(
                                b.body(),
                                r.headers()
                        )
                ));
        Tuple<String, Map<String, List<String>>> body = Await.result(fuBody, MAX_AWAIT);
        Assert.assertEquals(TestController.VERY_HUGE_TEXT + "\n", body._1);
        Assert.assertEquals("text/plain", body._2.get("Content-Type").flatMap(Traversable::headOption).getOrElse("none"));
    }

    @Test
    public void testJsonResult() throws Exception {
        // Thread.sleep(Duration.of("10min").toMillis());
        Future<Tuple<JsValue, Map<String, List<String>>>> fuBody = WS.host("http://localhost:9000")
                .withPath("/json")
                .withHeader("Api-Key", "12345")
                .call()
                .flatMap(r -> r.body().map(b ->
                        Tuple.of(
                                b.json(),
                                r.headers()
                        )
                ));
        Tuple<JsValue, Map<String, List<String>>> body = Await.result(fuBody, MAX_AWAIT);
        Assert.assertEquals(Json.obj().with("message", "Hello World!"), body._1);
        Assert.assertEquals("application/json", body._2.get("Content-Type").flatMap(Traversable::headOption).getOrElse("none"));
    }

    @Test
    public void testAsyncJsonResult() throws Exception {
        Future<Tuple<JsValue, Map<String, List<String>>>> fuBody = WS.host("http://localhost:9000")
                .withPath("/ws")
                .withHeader("Api-Key", "12345")
                .withQueryParam("q", "81.246.24.51")
                .call()
                .flatMap(r -> {
                    return r.body().map(b -> {
                        System.out.println(b.body());
                        return Tuple.of(
                                b.json(),
                                r.headers()
                        );
                    });
                });
        Tuple<JsValue, Map<String, List<String>>> body = Await.result(fuBody, MAX_AWAIT);
        JsObject jsonBody = body._1.asObject();
        System.out.println(jsonBody.pretty());
        Assert.assertTrue(jsonBody.exists("latitude"));
        Assert.assertTrue(jsonBody.exists("longitude"));
        Assert.assertTrue(jsonBody.exists("ip"));
        Assert.assertTrue(jsonBody.exists("city"));
        Assert.assertTrue(jsonBody.exists("country_name"));
        Assert.assertEquals("application/json", body._2.get("Content-Type").flatMap(Traversable::headOption).getOrElse("none"));
    }

    @Test
    public void testAsyncJsonResult2() throws Exception {
        Future<Tuple<JsValue, Map<String, List<String>>>> fuBody = WS.host("http://localhost:9000")
                .withPath("/ws2")
                .withHeader("Api-Key", "12345")
                .call()
                .flatMap(r -> r.body().map(b ->
                        Tuple.of(
                                b.json(),
                                r.headers()
                        )
                ));
        Tuple<JsValue, Map<String, List<String>>> body = Await.result(fuBody, MAX_AWAIT);
        JsObject jsonBody = body._1.asObject();
        System.out.println(jsonBody.pretty());
        Assert.assertTrue(jsonBody.exists("latitude"));
        Assert.assertTrue(jsonBody.exists("longitude"));
        Assert.assertTrue(jsonBody.exists("ip"));
        Assert.assertTrue(jsonBody.exists("city"));
        Assert.assertTrue(jsonBody.exists("country_name"));
        Assert.assertEquals("application/json", body._2.get("Content-Type").flatMap(Traversable::headOption).getOrElse("none"));
    }

    @Test
    public void testPostJsonResult() throws Exception {
        String uuid = UUID.randomUUID().toString();
        Future<Tuple<JsValue, Map<String, List<String>>>> fuBody = WS.host("http://localhost:9000")
                .withPath("/post")
                .withMethod(HttpMethods.POST)
                .withHeader("Api-Key", "12345")
                .withHeader("Content-Type", "application/json")
                .withBody(Json.obj().with("uuid", uuid))
                .call().flatMap(r -> r.body().map(b ->
                        Tuple.of(
                                b.json(),
                                r.headers()
                        )
                ));
        Tuple<JsValue, Map<String, List<String>>> body = Await.result(fuBody, MAX_AWAIT);
        Assert.assertEquals(Json.obj().with("uuid", uuid).with("processed_by", "SB"), body._1);
    }

    @Test
    public void testHtmlResult() throws Exception {
        Future<Tuple<String, Map<String, List<String>>>> fuBody = WS.host("http://localhost:9000")
                .withPath("/html")
                .withHeader("Api-Key", "12345")
                .call()
                .flatMap(r -> r.body().map(b ->
                        Tuple.of(
                                b.body(),
                                r.headers()
                        )
                ));
        Tuple<String, Map<String, List<String>>> body = Await.result(fuBody, MAX_AWAIT);
        Assert.assertEquals("<h1>Hello World!</h1>", body._1);
        Assert.assertEquals("text/html", body._2.get("Content-Type").flatMap(Traversable::headOption).getOrElse("none"));
    }

    @Test
    public void testTemplateResult() throws Exception {
        Future<Tuple<String, Map<String, List<String>>>> fuBody = WS.host("http://localhost:9000")
                .withPath("/template")
                .withHeader("Api-Key", "12345")
                .call()
                .flatMap(r -> r.body().map(b ->
                        Tuple.of(
                                b.body(),
                                r.headers()
                        )
                ));
        Tuple<String, Map<String, List<String>>> body = Await.result(fuBody, MAX_AWAIT);
        Assert.assertEquals("<div><h1>Hello Mathieu!</h1></div>", body._1);
        Assert.assertEquals("text/html", body._2.get("Content-Type").flatMap(Traversable::headOption).getOrElse("none"));
    }

    @Test
    public void testAssets() throws Exception {
        Future<Tuple<String, Map<String, List<String>>>> fuBody = WS.host("http://localhost:9000")
                .withPath("/assets/test.txt")
                .call()
                .flatMap(r -> r.body().map(b ->
                        Tuple.of(
                                b.body(),
                                r.headers()
                        )
                ));
        Tuple<String, Map<String, List<String>>> body = Await.result(fuBody, MAX_AWAIT);
        Assert.assertEquals("test", body._1);
        Assert.assertEquals("text/plain", body._2.get("Content-Type").flatMap(Traversable::headOption).getOrElse("none"));
    }

    @Test
    public void testSSEResult() throws Exception {
        Future<Tuple<String, Map<String, List<String>>>> fuBody = WS.host("http://localhost:9000")
                .withPath("/sse")
                .withHeader("Api-Key", "12345")
                .call()
                .flatMap(r -> r.body().map(b ->
                        Tuple.of(
                                b.body(),
                                r.headers()
                        )
                ));
        Tuple<String, Map<String, List<String>>> body = Await.result(fuBody, MAX_AWAIT);
        java.util.List<JsObject> parts = Arrays.asList(body._1.split("\n"))
                .stream()
                .filter(s -> !s.trim().isEmpty())
                .map(s -> s.replace("data: ", ""))
                .map(s -> Json.parse(s).asObject())
                .collect(Collectors.toList());
        for (JsObject obj : parts) {
            Assert.assertTrue(obj.exists("value"));
            Assert.assertTrue(obj.exists("time"));
        }
        Assert.assertTrue(parts.size() < 7);
        Assert.assertEquals("text/event-stream", body._2.get("Content-Type").flatMap(Traversable::headOption).getOrElse("none"));
    }

    @Test
    public void testSSEResultWitActor() throws Exception {
        Future<Tuple<String, Map<String, List<String>>>> fuBody = WS.host("http://localhost:9000")
                .withPath("/sse2")
                .call()
                .flatMap(r -> r.body().map(b ->
                        Tuple.of(
                                b.body(),
                                r.headers()
                        )
                ));
        Tuple<String, Map<String, List<String>>> body = Await.result(fuBody, MAX_AWAIT);
        java.util.List<JsObject> parts = Arrays.asList(body._1.split("\n"))
                .stream()
                .filter(s -> !s.trim().isEmpty())
                .map(s -> s.replace("data: ", ""))
                .map(s -> Json.parse(s).asObject())
                .collect(Collectors.toList());
        for (JsObject obj : parts) {
            Assert.assertTrue(obj.exists("Hello"));
            Assert.assertEquals(obj.field("Hello").asString(), "World!");
        }
        Assert.assertEquals(3, parts.size());
        Assert.assertEquals("text/event-stream", body._2.get("Content-Type").flatMap(Traversable::headOption).getOrElse("none"));
    }
}
