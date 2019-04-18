package com.manikanta;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.net.OpenSSLEngineOptions;
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
public class HttpClientExample {

    private static Vertx vertx;
    private static final Logger LOG = LoggerFactory.getLogger(HttpClientExample.class);

    @BeforeClass
    public static void setup(TestContext context) {
        vertx = Vertx.vertx();
    }

    @AfterClass
    public static void teardown() {
        vertx.close();
    }


    // Note: Run HttpServerExample before running this example
    @Test
    public void test_raw_http_client(TestContext context) {
        Async async = context.async();

        HttpClientOptions options = prepareHttpClientOptions();

        HttpClient httpClient = vertx.createHttpClient(options);

//        String absUrl = "https://api.ipify.org/?format=string";
//        String absUrl = "http://localhost:9999/";
        String absUrl = "https://abs.twimg.com/favicon.ico";

        httpClient
            .getAbs(absUrl, response -> {
                response.bodyHandler(body -> {
                    String bodyStr = body.toString();

                    LOG.info("\nReceived response -->\n\tProtocol: {}\n\tStatus: {}\n\tContent: '{}...'",
                             response.version(),
                             response.statusCode(),
                             bodyStr.substring(0, Math.min(bodyStr.length(), 50)));

                    context.assertEquals(HttpVersion.HTTP_2, response.version());
                    context.assertEquals(200, response.statusCode());

                    async.complete();
                });
            })
            .exceptionHandler(ex -> {
                LOG.error("Error while getting response", ex);
                context.fail(ex);
            })
            .end();

        async.await(5_000);
    }


    private HttpClientOptions prepareHttpClientOptions() {
        return new HttpClientOptions()
            // HTTP/2 config
            .setProtocolVersion(HttpVersion.HTTP_2)
            .setUseAlpn(true)
            .setHttp2MultiplexingLimit(1000)
            .setHttp2MaxPoolSize(4)

//            .setSsl(true) // Required for HTTP/2
            // Using BoringSSL (Google's fork of OpenSSL) instead of JDK for better performance
            .setOpenSslEngineOptions(new OpenSSLEngineOptions())
            .setTrustAll(true) // ----> only for testing!!!

            .setMaxPoolSize(5) // Enabling connection pooling

            .setKeepAlive(true)
            .setIdleTimeout(60)

            .setPipelining(true) // This helps a lot especially with HTTP/1.1 for small payloads (~1KB)

            .setTcpNoDelay(true)

            // Native transport config
            .setTcpCork(true)
            .setTcpFastOpen(true)
            .setTcpKeepAlive(true)
            .setTcpQuickAck(true)

            .setUsePooledBuffers(true)

//            .setLogActivity(true)
            .setTryUseCompression(true)

            // Proxy config
//            .setProxyOptions(new ProxyOptions().setHost("localhost").setPort(8888))
            ;
    }
}
