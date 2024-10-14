package org.opentripplanner.framework.io;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import org.glassfish.jersey.server.internal.routing.UriRoutingContext;
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
}
