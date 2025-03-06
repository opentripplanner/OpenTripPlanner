package org.opentripplanner.inspector.vector;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.opentripplanner.TestServerContext;
import org.opentripplanner.inspector.vector.geofencing.GeofencingZonesLayerBuilder;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.transit.service.TimetableRepository;

class VectorTileResponseFactoryTest {

  public static final OtpServerRequestContext SERVER_CONTEXT =
    TestServerContext.createServerContext(new Graph(), new TimetableRepository());

  enum LayerType {
    RED,
    GREEN,
  }

  private record LayerParams(String name, LayerType type) implements LayerParameters<LayerType> {
    @Override
    public String mapper() {
      return "Colors";
    }
  }

  private static final List<LayerParameters<LayerType>> LAYERS = List.of(
    new LayerParams("red", LayerType.RED),
    new LayerParams("green", LayerType.GREEN)
  );

  private static LayerBuilder<?> createLayerBuilder(
    LayerParameters<LayerType> layerParameters,
    Locale locale,
    OtpServerRequestContext context
  ) {
    return new GeofencingZonesLayerBuilder(context.graph(), layerParameters);
  }

  private static Response computeResponse(List<String> layers) {
    return VectorTileResponseFactory.create(
      1,
      1,
      1,
      Locale.ENGLISH,
      layers,
      LAYERS,
      VectorTileResponseFactoryTest::createLayerBuilder,
      SERVER_CONTEXT
    );
  }

  @Test
  void return404WhenAllLayersNotFound() {
    var resp = computeResponse(List.of("yellow", "blue"));

    assertEquals(404, resp.getStatus());
    assertEquals("text/plain; charset=UTF-8", resp.getHeaderString(HttpHeaders.CONTENT_TYPE));
    assertEquals(
      "Could not find vector tile layer(s). Requested layers: [yellow, blue]. Available layers: [red, green].",
      resp.getEntity()
    );
  }

  @Test
  void return404WhenOneLayerNotFound() {
    var resp = computeResponse(List.of("red", "blue"));

    assertEquals(404, resp.getStatus());
    assertEquals("text/plain; charset=UTF-8", resp.getHeaderString(HttpHeaders.CONTENT_TYPE));
    assertEquals(
      "Could not find vector tile layer(s). Requested layers: [red, blue]. Available layers: [red, green].",
      resp.getEntity()
    );
  }

  @Test
  void return200WhenAllLayersFound() {
    var resp = computeResponse(List.of("red", "green"));
    // framework will take care of setting it
    assertEquals(null, resp.getHeaderString(HttpHeaders.CONTENT_TYPE));
    assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());
  }
}
