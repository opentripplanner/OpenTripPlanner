package org.opentripplanner.inspector.vector;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.opentripplanner.TestServerContext;
import org.opentripplanner.inspector.vector.geofencing.GeofencingZonesLayerBuilder;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.transit.service.TransitModel;

class VectorTileResponseFactoryTest {

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

  @Test
  void return404WhenLayerNotFound() {
    var x = VectorTileResponseFactory.create(
      1,
      1,
      1,
      Locale.ENGLISH,
      List.of("yellow", "blue"),
      LAYERS,
      VectorTileResponseFactoryTest::createLayerBuilder,
      TestServerContext.createServerContext(new Graph(), new TransitModel())
    );

    assertEquals(404, x.getStatus());
    assertEquals(
      "Could not find vector tile layer(s). Requested layers: [yellow, blue]. Available layers: [red, green].",
      x.getEntity()
    );
  }

  @Test
  void return200WhenAllLayersFound() {
    var x = VectorTileResponseFactory.create(
      1,
      1,
      1,
      Locale.ENGLISH,
      List.of("red", "green"),
      LAYERS,
      VectorTileResponseFactoryTest::createLayerBuilder,
      TestServerContext.createServerContext(new Graph(), new TransitModel())
    );

    assertEquals(Response.Status.OK.getStatusCode(), x.getStatus());
  }
}
