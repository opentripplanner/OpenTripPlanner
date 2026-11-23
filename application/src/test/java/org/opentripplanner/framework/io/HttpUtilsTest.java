package org.opentripplanner.framework.io;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import org.glassfish.jersey.server.internal.routing.UriRoutingContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.test.support.HttpForTest;

class HttpUtilsTest {

  private static final String HOSTNAME = "example.com";

  static List<String> testCases() {
    return List.of(
      HOSTNAME,
      "example.com,",
      " example.com ,",
      "example.com,example.com",
      "example.com, example.com",
      "example.com, example.net",
      "example.com, example.net, example.com",
      " example.com,    example.net,   example.com"
    );
  }

  @ParameterizedTest
  @MethodSource("testCases")
  void extractHost(String headerValue) {
    var req = HttpForTest.containerRequest();
    req.headers(Map.of("X-Forwarded-Host", List.of(headerValue)));
    var uriInfo = new UriRoutingContext(req);
    var baseAddress = HttpUtils.getBaseAddress(uriInfo, req);
    assertEquals("https://" + HOSTNAME, baseAddress);
  }

  @Test
  void resolveBaseUrl() {
    var host = "http://example.com";
    assertEquals(
      "http://example.com/otp/debug/vectortiles",
      HttpUtils.resolveBaseUrl(host, "otp/debug/vectortiles")
    );
    // leading and trailing slashes of a path are optional
    for (var basePath : List.of("/gateway/tiles", "gateway/tiles/", "/gateway/tiles/")) {
      assertEquals("http://example.com/gateway/tiles", HttpUtils.resolveBaseUrl(host, basePath));
    }
    // a full URL is used as is, apart from a trailing slash
    assertEquals(
      "https://api.example.com/tiles",
      HttpUtils.resolveBaseUrl(host, "https://api.example.com/tiles/")
    );
    assertEquals(
      "HTTP://api.example.com/tiles",
      HttpUtils.resolveBaseUrl(host, "HTTP://api.example.com/tiles")
    );
  }
}
