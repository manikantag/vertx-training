package com.manikanta;

import static com.manikanta.TestUtils.randomInt;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
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
public class EventBusExamples {

    private static Vertx vertx;
    private static final Logger LOG = LoggerFactory.getLogger(EventBusExamples.class);

    @BeforeClass
    public static void setup(TestContext context) {
        vertx = Vertx.vertx();
    }

    @AfterClass
    public static void teardown() {
        vertx.close();
    }


    @Test
    public void pub_sub(TestContext context) {
        Async async = context.strictAsync(10);

        EventBus eventBus = vertx.eventBus();

        for (int i = 0; i < 10; i++) {
            eventBus.localConsumer("stocks.google", message -> {
                LOG.info("New Google stock info: " + message.body());
                async.countDown();
            });
        }

        vertx.setTimer(300, timerId -> {
            eventBus.publish("stocks.google", randomInt());
        });


        async.await(5_000);
    }


    @Test
    public void one_to_one(TestContext context) {
        Async async = context.strictAsync(1);

        EventBus eventBus = vertx.eventBus();

        for (int i = 0; i < 10; i++) {
            eventBus.localConsumer("stocks.google", message -> {
                LOG.info("New Google stock info: " + message.body());
                async.countDown();
            });
        }

        vertx.setTimer(300, timerId -> {
            eventBus.send("stocks.google", randomInt());
        });


        async.await(5_000);
    }


    @Test
    public void request_response(TestContext context) {
        Async async = context.async();

        EventBus eventBus = vertx.eventBus();

        eventBus.localConsumer("chatroom_1", message -> {
            LOG.info("Message from publisher: " + message.body());
            message.reply("Subscriber message " + randomInt());
        });

        vertx.setPeriodic(300, timerId -> {
            eventBus.send("chatroom_1",
                          "Publisher message " + randomInt(),
                          replyAR -> {
                              if (replyAR.succeeded()) {
                                  Message<Object> replyMessage = replyAR.result();
                                  LOG.info("Message from subscriber: " + replyMessage.body());
                              } else {
                                  LOG.error("Failed to receive reply");
                                  context.fail(replyAR.cause());
                              }
                          });
        });


        async.await(3_000);
    }

}
