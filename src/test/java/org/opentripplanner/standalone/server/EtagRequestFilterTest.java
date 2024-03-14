package org.opentripplanner.standalone.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.framework.io.HttpUtils.APPLICATION_X_PROTOBUF;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.glassfish.jersey.message.internal.OutboundJaxrsResponse;
import org.glassfish.jersey.message.internal.OutboundMessageContext;
import org.glassfish.jersey.message.internal.Statuses;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;
import org.jets3t.service.utils.Mimetypes;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.test.support.HttpForTest;

class EtagRequestFilterTest {

  static final String vectorTilesResponse = "some vector tiles";
  static final String vectorTilesEtag = "\"20c17790\"";

  static Stream<Arguments> etagCases() {
    return Stream.of(
      Arguments.of("GET", 200, APPLICATION_X_PROTOBUF, bytes(vectorTilesResponse), vectorTilesEtag),
      Arguments.of("GET", 404, APPLICATION_X_PROTOBUF, bytes("hello123"), null),
      Arguments.of("GET", 200, "application/json", bytes("{}"), null),
      Arguments.of("POST", 200, APPLICATION_X_PROTOBUF, bytes("hello123"), null),
      Arguments.of("GET", 200, APPLICATION_X_PROTOBUF, bytes(""), null),
      Arguments.of("POST", 200, Mimetypes.MIMETYPE_HTML, bytes("<body></body>"), null)
    );
  }

  @ParameterizedTest(
    name = "{0} request with response status={1} type={2}, entity={3} produces ETag header {4}"
  )
  @MethodSource("etagCases")
  void writeEtag(
    String method,
    int status,
    String responseContentType,
    byte[] entity,
    String expectedEtag
  ) throws IOException {
    var request = HttpForTest.containerRequest(method);
    var response = response(status, request);
    var headers = response.getHeaders();
    headers.add(EtagRequestFilter.HEADER_CONTENT_TYPE, responseContentType);
    response.setEntity(entity);

    var filter = new EtagRequestFilter();
    filter.filter(request, response);

    assertEquals(expectedEtag, response.getHeaderString(EtagRequestFilter.HEADER_ETAG));
  }

  static Stream<Arguments> ifNoneMatchCases() {
    return Stream.of(
      Arguments.of("XXX", 200, bytes(vectorTilesResponse)),
      Arguments.of(vectorTilesEtag, 304, null)
    );
  }

  @ParameterizedTest(name = "If-None-Match header of {0} should lead to a status code of {2}")
  @MethodSource("ifNoneMatchCases")
  void ifNoneMatch(String ifNoneMatch, int expectedStatus, byte[] expectedEntity)
    throws IOException {
    var request = HttpForTest.containerRequest("GET");
    request.header(EtagRequestFilter.HEADER_IF_NONE_MATCH, ifNoneMatch);
    var response = response(200, request);
    var headers = response.getHeaders();
    headers.add(EtagRequestFilter.HEADER_CONTENT_TYPE, APPLICATION_X_PROTOBUF);
    var bytes = bytes(vectorTilesResponse);
    response.setEntity(bytes);

    var filter = new EtagRequestFilter();
    filter.filter(request, response);

    assertEquals(expectedStatus, response.getStatus());
    assertArrayEquals(expectedEntity, (byte[]) response.getEntity());
  }

  @Nonnull
  private static ContainerResponse response(int status, ContainerRequest request) {
    return new ContainerResponse(
      request,
      new OutboundJaxrsResponse(Statuses.from(status), new OutboundMessageContext())
    );
  }

  @Nonnull
  private static byte[] bytes(String input) {
    return input.getBytes(StandardCharsets.UTF_8);
  }
}
