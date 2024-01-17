package org.opentripplanner.apis.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.URISyntaxException;
import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.internal.routing.UriRoutingContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TileJsonTest {

  @ParameterizedTest
  @ValueSource(
    strings = {
      "/otp_ct/vectorTiles",
      "otp_ct/vectorTiles/",
      "otp_ct/vectorTiles///",
      "///otp_ct/vectorTiles/",
    }
  )
  void overrideBasePath(String basePath) throws URISyntaxException {
    var uri = new URI("https://localhost:8080");
    var header = new ContainerRequest(uri, uri, "GET", null, new MapPropertiesDelegate(), null);
    var uriInfo = new UriRoutingContext(header);
    assertEquals(
      "https://localhost:8080/otp_ct/vectorTiles/stops,rentalVehicles/{z}/{x}/{y}.pbf",
      TileJson.overrideBasePath(uriInfo, header, basePath, "stops,rentalVehicles")
    );
  }
}
