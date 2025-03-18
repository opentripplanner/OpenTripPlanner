package org.opentripplanner.apis.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.List;
import org.glassfish.jersey.server.internal.routing.UriRoutingContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.service.worldenvelope.model.WorldEnvelope;
import org.opentripplanner.test.support.HttpForTest;

class TileJsonTest {

  private static final List<String> LAYERS = List.of("stops", "rentalVehicles");
  private static final WorldEnvelope ENVELOPE = WorldEnvelope.of()
    .expandToIncludeStreetEntities(1, 1)
    .expandToIncludeStreetEntities(2, 2)
    .build();
  private static final FeedInfo FEED_INFO = new FeedInfo(
    "1",
    "Trimet",
    "https://trimet.org",
    "en",
    LocalDate.MIN,
    LocalDate.MIN,
    "1"
  );

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

  @Test
  void attributionFromFeedInfo() {
    var tileJson = new TileJson("http://example.com", ENVELOPE, List.of(FEED_INFO));
    assertEquals("<a href='https://trimet.org'>Trimet</a>", tileJson.attribution);
  }

  @Test
  void duplicateAttribution() {
    var tileJson = new TileJson("http://example.com", ENVELOPE, List.of(FEED_INFO, FEED_INFO));
    assertEquals("<a href='https://trimet.org'>Trimet</a>", tileJson.attribution);
  }

  @Test
  void attributionFromOverride() {
    var override = "OVERRIDE";
    var tileJson = new TileJson("http://example.com", ENVELOPE, override);
    assertEquals(override, tileJson.attribution);
  }
}
