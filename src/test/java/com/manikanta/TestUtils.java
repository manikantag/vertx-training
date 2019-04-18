package com.manikanta;

import java.util.Random;

import io.vertx.core.Future;
import io.vertx.core.Vertx;

class TestUtils {
    static <T> Future<T> mimicAsyncOp(Vertx vertx, int delay, T result) {
        Future<T> future = Future.future();
        vertx.setTimer(delay, timerId -> future.complete(result));
        return future;
    }

    static Random random = new Random();

    static int randomInt() {
        return random.nextInt(10_000);
    }
}
