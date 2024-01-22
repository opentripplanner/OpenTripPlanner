package org.opentripplanner.apis.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.glassfish.jersey.server.internal.routing.UriRoutingContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentripplanner.test.support.HttpForTest;

class TileJsonTest {

  private static final List<String> LAYERS = List.of("stops", "rentalVehicles");

  @ParameterizedTest
  @ValueSource(
    strings = {
      "/otp_ct/vectorTiles",
      "otp_ct/vectorTiles/",
      "otp_ct/vectorTiles///",
      "///otp_ct/vectorTiles/",
    }
  )
  void overrideBasePath(String basePath) {
    var req = HttpForTest.containerRequest();
    var uriInfo = new UriRoutingContext(req);
    assertEquals(
      "https://localhost:8080/otp_ct/vectorTiles/stops,rentalVehicles/{z}/{x}/{y}.pbf",
      TileJson.urlFromOverriddenBasePath(uriInfo, req, basePath, LAYERS)
    );
  }

  @Test
  void defaultPath() {
    var req = HttpForTest.containerRequest();
    var uriInfo = new UriRoutingContext(req);
    assertEquals(
      "https://localhost:8080/otp/routers/default/vectorTiles/stops,rentalVehicles/{z}/{x}/{y}.pbf",
      TileJson.urlWithDefaultPath(uriInfo, req, LAYERS, "default", "vectorTiles")
    );
  }
}
