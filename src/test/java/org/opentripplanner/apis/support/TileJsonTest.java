package org.opentripplanner.apis.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.URISyntaxException;
import javax.annotation.Nonnull;
import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.internal.routing.UriRoutingContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TileJsonTest {

  private static final String LAYERS = "stops,rentalVehicles";

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
    var req = container();
    var uriInfo = new UriRoutingContext(req);
    assertEquals(
      "https://localhost:8080/otp_ct/vectorTiles/stops,rentalVehicles/{z}/{x}/{y}.pbf",
      TileJson.overrideBasePath(uriInfo, req, basePath, LAYERS)
    );
  }

  @Test
  void defaultPath() throws URISyntaxException {
    var req = container();
    var uriInfo = new UriRoutingContext(req);
    assertEquals(
      "https://localhost:8080/otp/routers/default/vectorTiles/stops,rentalVehicles/{z}/{x}/{y}.pbf",
      TileJson.defaultPath(uriInfo, req, LAYERS, "default", "vectorTiles")
    );
  }

  @Nonnull
  private static ContainerRequest container() throws URISyntaxException {
    var uri = new URI("https://localhost:8080");
    return new ContainerRequest(uri, uri, "GET", null, new MapPropertiesDelegate(), null);
  }
}
