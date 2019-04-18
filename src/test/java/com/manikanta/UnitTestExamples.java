package com.manikanta;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
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
public class UnitTestExamples {

    private static Vertx vertx;
    private static final Logger LOG = LoggerFactory.getLogger(UnitTestExamples.class);

    @BeforeClass
    public static void setup(TestContext context) {
        vertx = Vertx.vertx();
    }

    @AfterClass
    public static void teardown() {
        vertx.close();
    }


    @Test
    public void passing_test(TestContext context) {
        Async async = context.async();

        vertx.setTimer(500, timerId -> {
            async.complete();
        });

        async.awaitSuccess();
    }


    @Test
    public void failing_test(TestContext context) {
        Async async = context.async();

        vertx.setTimer(500, timerId -> {
            context.fail("Some error message");
        });

        async.awaitSuccess();
    }


    @Test
    public void timed_test(TestContext context) {
        Async async = context.async();

        // Intentionally not completing or failing

        async.await(5_000);
    }


    @Test
    public void multiple_tests(TestContext context) {
        int totalEmployees = 10;

        Async async = context.async(totalEmployees);
        // Async async = context.strictAsync(totalEmployees); // Strict variant - countDown() can't be called when < 0

        for (int i = 1; i <= totalEmployees; i++) {
            int empId = i;

            getEmployeeSalary(empId).setHandler(salaryAR -> {
                if (salaryAR.succeeded()) {
                    LOG.info("{} salary = {}", empId, salaryAR.result());
                    context.assertEquals(empId * 100, salaryAR.result());
                    async.countDown();
                } else {
                    context.fail(salaryAR.cause());
                }
            });
        }

        async.await(2_000);
    }

    private Future<Integer> getEmployeeSalary(int empId) {
        Future<Integer> future = Future.future();
        vertx.setTimer(100, timerId -> future.complete(empId * 100));
        return future;
    }

}
