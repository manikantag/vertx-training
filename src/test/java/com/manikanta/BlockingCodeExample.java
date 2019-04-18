package com.manikanta;

import java.util.concurrent.TimeUnit;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;
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
public class BlockingCodeExample {

    private static Vertx vertx;
    private static final Logger LOG = LoggerFactory.getLogger(BlockingCodeExample.class);


    @BeforeClass
    public static void setup(TestContext context) {
        vertx = Vertx.vertx();
    }

    @AfterClass
    public static void teardown() {
        vertx.close();
    }


    @Test
    public void run_blocking_code_wrong_way(TestContext context) {
        Async async = context.async();

        LOG.info("Verticle running (before blocking op)...");

        int result = someLongRunningOperation();
        LOG.info("Long running operation result: {}", result);

        vertx.setPeriodic(500, timerId -> LOG.info("Verticle running (after blocking op)..."));

        async.await(10_000);
    }


    @Test
    public void run_blocking_code_right_way_using_executeBlocking(TestContext context) {
        Async async = context.async();

        LOG.info("Verticle running (before blocking op)...");

        // By default blocking code is executed on the Vert.x worker pool which
        // will be used several other Vert.x internal & public functionality
        vertx.executeBlocking(future -> {
            int result = someLongRunningOperation();
            LOG.info("Long running operation result: {}", result);

            // Once blocking operations are completed, passed 'future'
            // needs to be completed to let Vertx know
            future.complete();
        }, res -> {
            LOG.info("executeBlocking completed");
        });

        vertx.setPeriodic(500, timerId -> LOG.info("Verticle running (after blocking op)..."));

        async.await(10_000);
    }


    @Test
    public void run_blocking_code_right_way_with_custom_worker_pool(TestContext context) {
        Async async = context.async();

        LOG.info("Verticle running (before blocking op)...");

        // Custom worker pool just for this blocking operations
        WorkerExecutor executor = vertx.createSharedWorkerExecutor("custom-blocking-worker-pool",
                                                                   2,
                                                                   10,
                                                                   TimeUnit.SECONDS);

        executor.executeBlocking(future -> {
            int result = someLongRunningOperation();
            LOG.info("Long running operation result: {}", result);

            // Once blocking operations are completed, passed 'future'
            // needs to be completed to let Vertx know
            future.complete();
        }, res -> {
            LOG.info("executeBlocking completed");

            executor.close(); // ----> Important: close when not required anymore
        });

        vertx.setPeriodic(500, timerId -> LOG.info("Verticle running (after blocking op)..."));

        async.await(10_000);
    }


    @Test
    public void run_blocking_code_right_way_using_worker_verticle(TestContext context) {
        Async async = context.async();

        LOG.info("Verticle running (before blocking op)...");

        // If major application logic involves blocking code, using Worker Verticle is
        // better as it separates the blocking code from main verticle (event loop) code
        vertx.deployVerticle(new TestBlockingWorkerVerticle(),
                             new DeploymentOptions()
                                 .setWorker(true)
        );

        vertx.eventBus()
             .send("blocking-ops",
                   "someargs",
                   replyAR -> {
                       LOG.info("Long running operation result: {}", replyAR.result().body());
                   });

        vertx.setPeriodic(500, timerId -> LOG.info("Verticle running (after blocking op)..."));

        async.await(10_000);
    }

    private class TestBlockingWorkerVerticle extends AbstractVerticle {
        @Override
        public void start() throws Exception {
            vertx.eventBus()
                 .localConsumer("blocking-ops",
                                message -> {
                                    int result = someLongRunningOperation();
                                    message.reply(result);
                                });
        }
    }


    private int someLongRunningOperation() {
        try {
            LOG.info("Sleeping {} thread for 5 sec", Thread.currentThread()
                                                           .getName());
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return 1_000;
    }
}
