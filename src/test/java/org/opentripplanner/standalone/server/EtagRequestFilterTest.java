package org.opentripplanner.standalone.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.message.internal.OutboundJaxrsResponse;
import org.glassfish.jersey.message.internal.OutboundMessageContext;
import org.glassfish.jersey.message.internal.Statuses;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.vectortiles.VectorTilesResource;

class EtagRequestFilterTest {

  @Test
  void writeEtag() throws IOException {
    var request = new ContainerRequest(null, null, "GET", null, new MapPropertiesDelegate(), null);

    var response = new ContainerResponse(
      request,
      new OutboundJaxrsResponse(Statuses.from(200), new OutboundMessageContext())
    );
    response
      .getHeaders()
      .add(EtagRequestFilter.HEADER_CONTENT_TYPE, VectorTilesResource.APPLICATION_X_PROTOBUF);
    response.setEntity("hello123".getBytes(StandardCharsets.UTF_8));

    var filter = new EtagRequestFilter();
    filter.filter(request, response);

    assertEquals(
      "\"0f30aa7a662c728b7407c54ae6bfd27d1\"",
      response.getHeaderString(EtagRequestFilter.HEADER_ETAG)
    );
  }
}
