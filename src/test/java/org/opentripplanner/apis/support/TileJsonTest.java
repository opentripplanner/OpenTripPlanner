package org.opentripplanner.apis.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.URISyntaxException;
import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.internal.routing.UriRoutingContext;
import org.junit.jupiter.api.Test;

class TileJsonTest {

  @Test
  void url() throws URISyntaxException {

    var uri= new URI("https://localhost:8080");
    var header = new ContainerRequest(uri, uri, "GET", null, new MapPropertiesDelegate(), null);
    var uriInfo =new UriRoutingContext(header);
    assertEquals("", TileJson.tileUrl(uriInfo, header,  "/OTP_CT/some/config/path/", "foo,bar"));
  }

}