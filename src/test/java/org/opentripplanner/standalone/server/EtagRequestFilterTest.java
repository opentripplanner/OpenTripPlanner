package org.opentripplanner.standalone.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.ext.vectortiles.VectorTilesResource.APPLICATION_X_PROTOBUF;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.message.internal.OutboundJaxrsResponse;
import org.glassfish.jersey.message.internal.OutboundMessageContext;
import org.glassfish.jersey.message.internal.Statuses;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;
import org.jets3t.service.utils.Mimetypes;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.opentripplanner.test.support.VariableSource;

class EtagRequestFilterTest {

  static Stream<Arguments> testCases = Stream.of(
    Arguments.of("GET", 200, APPLICATION_X_PROTOBUF, bytes("hello123"), "\"0c8fc868b\""),
    Arguments.of("GET", 404, APPLICATION_X_PROTOBUF, bytes("hello123"), null),
    Arguments.of("GET", 200, "application/json", bytes("{}"), null),
    Arguments.of("POST", 200, APPLICATION_X_PROTOBUF, bytes("hello123"), null),
    Arguments.of("GET", 200, APPLICATION_X_PROTOBUF, bytes(""), null),
    Arguments.of("POST", 200, Mimetypes.MIMETYPE_HTML, bytes("<body></body>"), null)
  );

  @ParameterizedTest(
    name = "{0} request with response status={1} type={2}, entity={3} produces ETag header {4}"
  )
  @VariableSource("testCases")
  void writeEtag(
    String method,
    int status,
    String responseContentType,
    byte[] entity,
    String expectedEtag
  ) throws IOException {
    var request = request(method);

    var response = new ContainerResponse(
      request,
      new OutboundJaxrsResponse(Statuses.from(status), new OutboundMessageContext())
    );
    var headers = response.getHeaders();
    headers.add(EtagRequestFilter.HEADER_CONTENT_TYPE, responseContentType);
    headers.add("Content-Length", entity.length);
    response.setEntity(entity);

    var filter = new EtagRequestFilter();
    filter.filter(request, response);

    assertEquals(expectedEtag, response.getHeaderString(EtagRequestFilter.HEADER_ETAG));
  }

  @Nonnull
  private static byte[] bytes(String input) {
    return input.getBytes(StandardCharsets.UTF_8);
  }

  @Nonnull
  private static ContainerRequest request(String method) {
    return new ContainerRequest(null, null, method, null, new MapPropertiesDelegate(), null);
  }
}
