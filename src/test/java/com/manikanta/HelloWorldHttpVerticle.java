package com.manikanta;

import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(VertxUnitRunner.class)
public class HelloWorldHttpVerticle {

    private static Vertx vertx;

    @BeforeClass
    public static void setup(TestContext context) {
        vertx = Vertx.vertx();

//        deployServerVerticle(context);
    }


    private static void deployServerVerticle(TestContext testContext) {
        Async async = testContext.async();

        vertx.deployVerticle(new HttpServerVerticle(),
                             ar -> {
                                 if (ar.succeeded()) async.complete();
                                 else testContext.fail();
                             });

        async.await(5_000);
    }


    @AfterClass
    public static void teardown() {
        vertx.close();
    }


    @Test
    public void test_server_for_success_response(TestContext context) throws Throwable {
        Async async = context.async(); // -----> one way to control async flow (alt: CountDownLatch, ...)

        vertx.createHttpClient()
             .getAbs("http://localhost:9999", response -> {
                 context.assertEquals(200,
                                      response.statusCode());

                 response.bodyHandler(body -> {
                     context.assertEquals("Hello world",
                                          body.toString());

                     response.request().connection().close(); // Close the connection (as not using pool here)
                     async.complete();
                 });
             })
             .end()
        ;

        async.await(5_000);
    }

}
