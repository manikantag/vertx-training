package com.manikanta;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.OpenSSLEngineOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpServerVerticle extends AbstractVerticle {

    private static Logger LOG = LoggerFactory.getLogger(HttpServerVerticle.class);


    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();

        vertx.deployVerticle(new HttpServerVerticle(), ar -> {
            if (ar.succeeded()) LOG.info("Verticle deployed");
            else LOG.error("Failed to deploy verticle", ar.cause());
        });
    }


    @Override
    public void start(Future<Void> startFuture) throws Exception {
        HttpServerOptions options = prepareHttpServerOptions();

        vertx.createHttpServer(options)
             .requestHandler(req -> {
                 req.response()
                    .putHeader("content-type", "text/plain")
                    .end("Hello world");
             })
             .listen(9999, ar -> {
                 if (ar.succeeded()) {
                     startFuture.complete();
                     LOG.info("HTTP server started on port 9999");
                 } else {
                     LOG.error("Deployment failed", ar.cause());
                     startFuture.fail(ar.cause());
                 }
             });
    }


    private HttpServerOptions prepareHttpServerOptions() {
        return new HttpServerOptions()
            // HTTP/2
//            .setUseAlpn(true) // h2 mode (HTTP/2 over secure socket); well supported and recommended
            .setUseAlpn(false) // h2c mode (HTTP/2 over plain socket); not supported well and not recommended

            // Using BoringSSL (Google's fork of OpenSSL) instead of JDK for better performance
            .setOpenSslEngineOptions(new OpenSSLEngineOptions())

//            .setLogActivity(true) // network activity only for debugging purposes; use setCompressionSupported(false)
            .setCompressionSupported(true)

            // TCP tuning
            .setTcpCork(true)
            .setTcpFastOpen(true)
            .setTcpNoDelay(true)
            .setTcpQuickAck(true)
            .setTcpKeepAlive(true)

            // Other
            // Close keep-alive connections after an idle time
            .setIdleTimeout(10)

            // SSL, Auth
//            .setSsl(true) // Required for HTTP/2
//            .setKeyStoreOptions(new JksOptions().setPath("keystore-location"))
//            .setClientAuth(ClientAuth.REQUIRED)
            ;
    }

}
