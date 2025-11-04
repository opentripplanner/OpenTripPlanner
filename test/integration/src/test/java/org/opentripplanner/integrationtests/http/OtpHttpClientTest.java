package org.opentripplanner.integrationtests.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import jakarta.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentripplanner.framework.io.OtpHttpClient;
import org.opentripplanner.framework.io.OtpHttpClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This integration test makes sure that Apache HTTP client still works with important hosts and
 * HTTP servers. It also verifies that the ResponseMapper interface correctly provides access to
 * HTTP headers.
 */
@Tag("integration")
class OtpHttpClientTest {

  private static final Logger LOG = LoggerFactory.getLogger(OtpHttpClientTest.class);
  private static final OtpHttpClient OTP_HTTP_CLIENT = new OtpHttpClientFactory().create(LOG);

  private static HttpServer server;
  private static int port;
  private static OtpHttpClient client;

  @BeforeAll
  static void setUp() throws IOException {
    // Start a simple HTTP server for testing header access
    server = HttpServer.create(new InetSocketAddress(0), 0);
    port = server.getAddress().getPort();

    // Endpoint that returns JSON with custom headers
    server.createContext("/json", exchange -> {
      String response = "{\"message\":\"Hello, World!\"}";
      exchange.getResponseHeaders().add("Content-Type", "application/json");
      exchange.getResponseHeaders().add("X-Custom-Header", "custom-value");
      exchange.getResponseHeaders().add("ETag", "\"12345\"");
      exchange.sendResponseHeaders(200, response.length());
      try (OutputStream os = exchange.getResponseBody()) {
        os.write(response.getBytes(StandardCharsets.UTF_8));
      }
    });

    // Endpoint with multiple values for same header
    server.createContext("/multi-header", exchange -> {
      String response = "multi-header response";
      exchange.getResponseHeaders().add("Set-Cookie", "cookie1=value1");
      exchange.getResponseHeaders().add("Set-Cookie", "cookie2=value2");
      exchange.getResponseHeaders().add("Set-Cookie", "cookie3=value3");
      exchange.sendResponseHeaders(200, response.length());
      try (OutputStream os = exchange.getResponseBody()) {
        os.write(response.getBytes(StandardCharsets.UTF_8));
      }
    });

    // Endpoint for testing cache headers
    server.createContext("/cache", exchange -> {
      String response = "cached content";
      exchange.getResponseHeaders().add("Cache-Control", "max-age=3600");
      exchange.getResponseHeaders().add("Last-Modified", "Wed, 21 Oct 2015 07:28:00 GMT");
      exchange.getResponseHeaders().add("ETag", "\"abc123\"");
      exchange.sendResponseHeaders(200, response.length());
      try (OutputStream os = exchange.getResponseBody()) {
        os.write(response.getBytes(StandardCharsets.UTF_8));
      }
    });

    // Endpoint for plain text
    server.createContext("/text", exchange -> {
      String response = "plain text response";
      exchange.getResponseHeaders().add("Content-Type", "text/plain");
      exchange.sendResponseHeaders(200, response.length());
      try (OutputStream os = exchange.getResponseBody()) {
        os.write(response.getBytes(StandardCharsets.UTF_8));
      }
    });

    server.start();

    client = new OtpHttpClientFactory().create(LOG);
  }

  @AfterAll
  static void tearDown() {
    if (server != null) {
      server.stop(0);
    }
  }

  @ParameterizedTest
  @ValueSource(
    strings = {
      // a few entur URLs
      "https://api.entur.io/mobility/v2/gbfs/",
      "https://storage.googleapis.com/marduk-production/outbound/gtfs/rb_sjn-aggregated-gtfs.zip",
      // Apache HTTP Client broke handling of S3 SSL certificates previously
      "https://s3.amazonaws.com/kcm-alerts-realtime-prod/tripupdates.pb",
    }
  )
  void httpGetRequest(String url) throws IOException {
    var uri = UriBuilder.fromUri(url).build();

    var stream = OTP_HTTP_CLIENT.getAsInputStream(uri, Duration.ofSeconds(30), Map.of());
    var bytes = IOUtils.toByteArray(stream);

    assertNotEquals(0, bytes.length, "Empty response body for %s".formatted(url));
  }

  @Test
  void shouldAccessResponseHeaders() {
    URI uri = URI.create("http://localhost:" + port + "/json");

    String result = client.getAndMap(uri, Map.of(), response -> {
      // Verify we can access headers
      assertTrue(response.header("Content-Type").isPresent());
      assertEquals("application/json", response.header("Content-Type").get());

      assertTrue(response.header("X-Custom-Header").isPresent());
      assertEquals("custom-value", response.header("X-Custom-Header").get());

      assertTrue(response.header("ETag").isPresent());
      assertEquals("\"12345\"", response.header("ETag").get());

      // Read the body
      try {
        return new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });

    assertEquals("{\"message\":\"Hello, World!\"}", result);
  }

  @Test
  void shouldAccessHeadersCaseInsensitively() {
    URI uri = URI.create("http://localhost:" + port + "/json");

    client.getAndMap(uri, Map.of(), response -> {
      // Test case insensitivity
      assertTrue(response.header("content-type").isPresent());
      assertTrue(response.header("CONTENT-TYPE").isPresent());
      assertTrue(response.header("Content-Type").isPresent());

      assertEquals(response.header("content-type").get(), response.header("CONTENT-TYPE").get());
      assertEquals(
        response.header("content-type").get(),
        response.header("etag").get() != null ? "application/json" : null
      );

      return null;
    });
  }

  @Test
  void shouldAccessMultiValueHeaders() {
    URI uri = URI.create("http://localhost:" + port + "/multi-header");

    client.getAndMap(uri, Map.of(), response -> {
      List<String> cookies = response.headerValues("Set-Cookie");
      assertNotNull(cookies);
      assertEquals(3, cookies.size());
      assertTrue(cookies.contains("cookie1=value1"));
      assertTrue(cookies.contains("cookie2=value2"));
      assertTrue(cookies.contains("cookie3=value3"));

      // header() should return first value
      assertTrue(response.header("Set-Cookie").isPresent());
      assertEquals("cookie1=value1", response.header("Set-Cookie").get());

      return null;
    });
  }

  @Test
  void shouldAccessCacheHeaders() {
    URI uri = URI.create("http://localhost:" + port + "/cache");

    client.getAndMap(uri, Map.of(), response -> {
      assertTrue(response.header("Cache-Control").isPresent());
      assertEquals("max-age=3600", response.header("Cache-Control").get());

      assertTrue(response.header("Last-Modified").isPresent());
      assertEquals("Wed, 21 Oct 2015 07:28:00 GMT", response.header("Last-Modified").get());

      assertTrue(response.header("ETag").isPresent());
      assertEquals("\"abc123\"", response.header("ETag").get());

      return null;
    });
  }

  @Test
  void shouldWorkWithGetAndMapAsJsonObject() {
    URI uri = URI.create("http://localhost:" + port + "/json");
    ObjectMapper mapper = new ObjectMapper();

    // This uses the helper method which internally uses ResponseMapper
    var result = client.getAndMapAsJsonObject(uri, Map.of(), mapper, Map.class);

    assertNotNull(result);
    assertEquals("Hello, World!", result.get("message"));
  }

  @Test
  void shouldProvideBodyStreamThatCanBeRead() {
    URI uri = URI.create("http://localhost:" + port + "/text");

    String result = client.getAndMap(uri, Map.of(), response -> {
      assertTrue(response.header("Content-Type").isPresent());
      assertEquals("text/plain", response.header("Content-Type").get());

      try {
        return new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });

    assertEquals("plain text response", result);
  }

  @Test
  void shouldProvideAllHeadersAsMap() {
    URI uri = URI.create("http://localhost:" + port + "/json");

    client.getAndMap(uri, Map.of(), response -> {
      Map<String, List<String>> headers = response.headers();
      assertNotNull(headers);

      // Should contain our custom headers (lowercased)
      assertTrue(headers.containsKey("content-type"));
      assertTrue(headers.containsKey("x-custom-header"));
      assertTrue(headers.containsKey("etag"));

      return null;
    });
  }

  @Test
  void shouldWorkWithTimeout() {
    URI uri = URI.create("http://localhost:" + port + "/json");

    String result = client.getAndMap(uri, Duration.ofSeconds(5), Map.of(), response -> {
      assertTrue(response.header("Content-Type").isPresent());
      try {
        return new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });

    assertNotNull(result);
  }

  @Test
  void shouldAccessStatusCode() {
    URI uri = URI.create("http://localhost:" + port + "/json");
    client.getAndMap(uri, Map.of(), response -> {
      assertEquals(200, response.statusCode());
      return null;
    });
  }
}
