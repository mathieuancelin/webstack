
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
import org.reactivecouchbase.concurrent.Promise;
import org.reactivecouchbase.functional.Tuple;
import org.reactivecouchbase.json.JsObject;
import org.reactivecouchbase.json.JsValue;
import org.reactivecouchbase.json.Json;
import org.reactivecouchbase.webstrack.BootstrappedContext;
import org.reactivecouchbase.webstrack.ws.WS;

public class AppTest {

    private static final Duration MAX_AWAIT = Duration.parse("4s");
    private static BootstrappedContext server;

    @BeforeClass
    public static void setUp() {
        server = new Routes().startApp();
    }

    @AfterClass
    public static void tearDown() {
        server.stopApp();
    }

    @Test
    public void testIndex() {
        Future<Tuple<Integer, Map<String, List<String>>>> fuBody = WS.host("http://localhost:9000")
                .withPath("/")
                .call()
                .flatMap(r -> r.body().map(b ->
                        Tuple.of(
                                r.status(),
                                r.headers()
                        )
                ));
        Tuple<Integer, Map<String, List<String>>> body = Await.result(fuBody, MAX_AWAIT);
        Assert.assertTrue(200 == body._1);
        Assert.assertEquals("text/html", body._2.get("Content-Type").flatMap(Traversable::headOption).getOrElse("none"));
    }
}