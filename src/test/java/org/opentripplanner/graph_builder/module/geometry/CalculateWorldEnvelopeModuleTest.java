package org.opentripplanner.graph_builder.module.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.transit.model._data.TransitModelForTest.stop;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.transit.model.site.RegularStop;

class CalculateWorldEnvelopeModuleTest {

  private final List<V> vertexes = List.of(new V(10d, 12d), new V(11d, 13d), new V(14d, 15d));

  private final List<RegularStop> stops = List.of(
    stop("1").withCoordinate(20d, 22d).build(),
    stop("1").withCoordinate(22d, 24d).build()
  );

  @Test
  void buildVertexesOnly() {
    var subject = CalculateWorldEnvelopeModule.build(vertexes, List.of());

    assertEquals(
      "WorldEnvelope{lowerLeft: (12.0, 10.0), upperRight: (15.0, 14.0), meanCenter: (13.5, 12.0)}",
      subject.toString()
    );
  }

  @Test
  void buildVertexesAndStops() {
    var subject = CalculateWorldEnvelopeModule.build(vertexes, stops);

    assertEquals(
      "WorldEnvelope{lowerLeft: (12.0, 10.0), upperRight: (22.0, 24.0), meanCenter: (17.0, 17.0), transitMedianCenter: (21.0, 23.0)}",
      subject.toString()
    );
  }

  static class V extends Vertex {

    protected V(double x, double y) {
      super(null, "V-" + x + "-" + y, x, y);
    }
  }
}
