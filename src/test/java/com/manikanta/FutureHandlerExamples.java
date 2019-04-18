package com.manikanta;

import static com.manikanta.TestUtils.mimicAsyncOp;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(VertxUnitRunner.class)
public class FutureHandlerExamples {

    private static Vertx vertx;
    private static final Logger LOG = LoggerFactory.getLogger(FutureHandlerExamples.class);

    @BeforeClass
    public static void setup(TestContext context) {
        vertx = Vertx.vertx();
    }

    @AfterClass
    public static void teardown() {
        vertx.close();
    }


    @Test
    public void test_future_success_failure(TestContext context) {
        Async async = context.async();

        Future<String> asyncFuture = Future.future();
        asyncFuture.setHandler(ar -> {
            if (ar.succeeded()) {
                LOG.info("Async operation result: {}", ar.result());
                async.complete();
            } else {
                LOG.error("Async operation failed", ar.cause());
                context.fail(ar.cause());
            }
        });

        // Simulate async operation
        vertx.setTimer(1_000,
                       timerId -> {
                           asyncFuture.complete("Success response");
//                           asyncFuture.fail("Failure reason");
                       });

        async.await(2_000);
    }


    @Test
    public void test_handler_usage(TestContext context) {
        Async async = context.async();

        doTaskWithHandler("dummy task",
                          asyncResult -> {
                              LOG.info("Task execution result is: {}", asyncResult);
                              async.complete();
                          },
                          error -> {
                              LOG.error("Task execution failed", error);
                              context.fail(error);
                          });

        async.awaitSuccess(2_000);
    }

    @Test
    public void test_future_usage(TestContext context) {
        Async async = context.async();

        Future<String> executionFuture = doTaskWithFuture("dummy task");
        executionFuture.setHandler(ar -> { // ar is short for AsyncResult
            if (ar.succeeded()) {
                LOG.info("Task execution result is: {}", ar.result());
                async.complete();
            } else {
                LOG.error("Task execution failed", ar.cause());
                context.fail(ar.cause());
            }
        });

        async.awaitSuccess(2_000);
    }

    private void doTaskWithHandler(String taskName, Handler<String> resultHandler, Handler<Exception> errorHandler) {
        vertx.setTimer(1_000, timerId -> {
            resultHandler.handle("Result_" + timerId);
        });
    }

    private Future<String> doTaskWithFuture(String taskName) {
        Future<String> resultFuture = Future.future();

        vertx.setTimer(1_000, timerId -> {
            resultFuture.complete("Result_" + timerId);
        });

        return resultFuture;
    }


    //----- Future composition example -----
    private Future<JsonObject> getUserPurchaseDataFromCRM() {
        Future<JsonObject> respFuture = Future.future();

        // Mimic async op
        vertx.setTimer(500, timerId -> {
            JsonObject userData = new JsonObject();
            userData.put("totalPurchase", 10_000);

            respFuture.complete(userData);
        });

        return respFuture;
    }

    private Future<JsonObject> getUserSocialInfoFromTwitter() {
        Future<JsonObject> respFuture = Future.future();

        // Mimic async op
        vertx.setTimer(400, timerId -> {
            JsonObject latestTweet = new JsonObject();
            latestTweet.put("latestTweet", "Not now, now I'm coding");

            respFuture.complete(latestTweet);
        });

        return respFuture;
    }

    // Concurrent composition: CompositeFuture.all(), CompositeFuture.any(), CompositeFuture.join()
    @Test
    public void test_orchestrate_concurrent_futures(TestContext context) {
        Async async = context.async();

        Future<JsonObject> userPurchases = getUserPurchaseDataFromCRM();
        Future<JsonObject> userSocialInfo = getUserSocialInfoFromTwitter();

        CompositeFuture.all(userPurchases, userSocialInfo)
                       .setHandler(ar -> {
                           if (ar.succeeded()) {
                               // Now we have both purchase data & social info
                               LOG.info("Purchase info: {}, Social info: {}",
                                        userPurchases,
                                        userSocialInfo);
                               async.complete();
                           } else {
                               // At least one the operation has failed
                               context.fail(ar.cause());
                           }
                       });

        // Other ways:
        // - Array of Futures when we doesn't know how the futures in advance:
        //       CompositeFuture.all(Arrays.asList(userPurchases, userSocialInfo))
        //
        // - Either FB or Twitter is enough:
        //       CompositeFuture.any(userSocialInfoFromFB, userSocialInfoFromTwitter)

        async.awaitSuccess(2_000);
    }


    // Sequential composition: Future.compose()
    @Test
    public void test_orchestrate_futures_in_sequence(TestContext context) {
        Async async = context.async();

        // Auth -> Get tweet -> Save tweet
        Future<String> authTokenFuture = mimicAsyncOp(vertx, 5_000,"JWT.Token");

        authTokenFuture
            .compose(authToken -> {
                LOG.info("Got auth token: {}", authToken);

                Future<String> tweetsFuture = mimicAsyncOp(vertx, 100, "This is my latest tweet");
                return tweetsFuture;
            })
            .setHandler(finalResultAR -> {
                if (finalResultAR.succeeded()) {
                    LOG.info("Got tweets: {}", finalResultAR.result());
                    async.complete();
                } else {
                    context.fail(finalResultAR.cause());
                }
            });

        async.awaitSuccess(2_000);
    }
}
