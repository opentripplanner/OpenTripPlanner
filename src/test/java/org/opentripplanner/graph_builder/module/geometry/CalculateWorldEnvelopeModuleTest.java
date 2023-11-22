package org.opentripplanner.graph_builder.module.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.model.vertex.VertexLabel;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.site.RegularStop;

class CalculateWorldEnvelopeModuleTest {

  private final TransitModelForTest testModel = TransitModelForTest.of();

  private final List<V> vertexes = List.of(new V(10d, 12d), new V(11d, 13d), new V(14d, 15d));

  private final List<RegularStop> stops = List.of(
    testModel.stop("1").withCoordinate(20d, 22d).build(),
    testModel.stop("1").withCoordinate(22d, 24d).build()
  );

  @Test
  void buildEmptyEnvelope() {
    var subject = CalculateWorldEnvelopeModule.build(List.of(), List.of());

    assertEquals(
      "WorldEnvelope{lowerLeft: (-90.0, -180.0), upperRight: (90.0, 180.0), meanCenter: (0.0, 0.0), transitMedianCenter: (47.101, 9.611)}",
      subject.toString()
    );
  }

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
      super(x, y);
    }

    @Override
    public VertexLabel getLabel() {
      return VertexLabel.string("%s/%s".formatted(getX(), getY()));
    }

    @Nonnull
    @Override
    public I18NString getName() {
      return I18NString.of(getLabel().toString());
    }
  }
}
