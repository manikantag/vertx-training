package com.manikanta;

import static com.manikanta.TestUtils.randomInt;

import java.time.LocalDateTime;
import java.util.Date;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(VertxUnitRunner.class)
public class HttpServerExample {

    private static Vertx vertx;
    private static final Logger LOG = LoggerFactory.getLogger(HttpServerExample.class);

    @BeforeClass
    public static void setup(TestContext context) {
        vertx = Vertx.vertx();
    }

    @AfterClass
    public static void teardown() {
        vertx.close();
    }


    @Test
    public void start_raw_http_server(TestContext context) {
        Async async = context.async();

        HttpServerOptions options = prepareHttpServerOptions();

        HttpServer httpServer = vertx.createHttpServer(options);

        httpServer
            .requestHandler(request -> {
                HttpServerResponse response = request.response();

                switch (request.path()) {
                    case "/date":
                        response.setChunked(true); // Or, Content-Length is required
                        response.write("Today date is ")
                                .write(new Date().toString())
                                .end();
                        break;
                    case "/time":
                        String time = getCurrentTime();
                        response.putHeader("Content-Length", String.valueOf(time.length())) // Or use setChunked()
                                .write(time)
                                .end();
                        break;
                    case "/rand":
                        // end(...) will sets the Content-Length automatically
                        response.end("Random number is " + String.valueOf(randomInt()));
                        break;
                    default:
                        response.setStatusCode(404)
                                .setStatusMessage("Not found")
                                .end("Not found");
                }

            })
            .listen(9999, listenAR -> {
                if (listenAR.succeeded()) {
                    LOG.info("HTTP Server has started on port 9999");
                } else {
                    LOG.error("Failed to start HTTP server", listenAR.cause());
                }
            });

        async.await(30_000);
    }


    // Benefits of vertx-web over raw HttpServer: https://vertx.io/docs/vertx-web/java
    @Test
    public void start_web_http_server(TestContext context) {
        Async async = context.async();

        HttpServerOptions options = prepareHttpServerOptions();

        HttpServer httpServer = vertx.createHttpServer(options); // Same till this point

        // Create a base router
        Router router = Router.router(vertx);

        // Additional routes can be created using regular expressions too
        // '/user/*' to match /user/123, /user/mani, but not /user/123/456
        // '/get/.*report' to match /get/pdf-report, /get/csv-report, but not /get/pdf-report/download or /get/pdf-file

        // Create all routes
        router.route(HttpMethod.GET, "/date") // Specifying the HTTP method
              .handler(routingContext -> {
                  routingContext.response()
                                .setChunked(true)
                                .write("Today date is ");

                  // Must call the next matching route
                  routingContext.next();
              });

        router.route(HttpMethod.GET, "/date") // Multiple routes for same path
              .method(HttpMethod.POST)
              .handler(routingContext -> {
                  routingContext.response()
                                .end(new Date().toString());
              });


        router.route("/time") // Defaulting to HTTP GET method
              .handler(routingContext -> {
                  routingContext.response()
                                .end(getCurrentTime());
              });

        router.get("/rand") // Using semantic methods
              .handler(this::randRouteHandler);

        // Vert.x web offers default 404 Handling

        // MIME based routes
//        router.post("/orders")
//              .consumes("application/json") // -----> Match by 'Content-Type' request header
//              .produces("application/json") // -----> Match by 'Accept' request header
//              .handler(rc -> {})
//              .failureHandler(frc -> {}); // -----> Route failure handler

        // Cookie support
//        router.route().handler(CookieHandler.create());

        // Session support
//        router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));



        httpServer.requestHandler(router)
                  .listen(9999, listenAR -> { // This part is same again
                      if (listenAR.succeeded()) {
                          LOG.info("HTTP Server has started on port 9999");
                      } else {
                          LOG.error("Failed to start HTTP server", listenAR.cause());
                      }
                  });

        async.await(300_000);
    }

    private void randRouteHandler(RoutingContext routingContext) {
        routingContext.response()
                      .end("Random number is " + randomInt());
    }


    private HttpServerOptions prepareHttpServerOptions() {
        return new HttpServerOptions()
            // HTTP/2
//            .setUseAlpn(true) // h2 mode (HTTP/2 over secure socket); well supported and recommended
//            .setUseAlpn(false) // h2c mode (HTTP/2 over plain socket); not supported well and not recommended

            // TCP tuning
            .setTcpCork(true)
            .setTcpFastOpen(true)
            .setTcpNoDelay(true)
            .setTcpQuickAck(true)
            .setTcpKeepAlive(true)

            // Other
            // Close keep-alive connections after an idle time
            .setIdleTimeout(10)
            .setCompressionSupported(true)

            // SSL, Auth
//            .setSsl(true) // Required for HTTP/2
//            .setKeyStoreOptions(new JksOptions().setPath("keystore-location"))
//            .setClientAuth(ClientAuth.REQUIRED)
            ;
    }


    private String getCurrentTime() {
        LocalDateTime now = LocalDateTime.now();
        return new StringBuilder("Current time is ")
            .append(now.getHour())
            .append(":")
            .append(now.getMinute())
            .append(":")
            .append(now.getSecond())
            .toString();
    }

}
