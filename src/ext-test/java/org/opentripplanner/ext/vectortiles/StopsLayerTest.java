package org.opentripplanner.ext.vectortiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.vectortiles.layers.stops.DigitransitStopPropertyMapper;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.trippattern.Deduplicator;
import org.opentripplanner.routing.vertextype.TransitStopVertexBuilder;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.site.Stop;
import org.opentripplanner.transit.service.StopModel;
import org.opentripplanner.transit.service.TransitModel;

public class StopsLayerTest {

  private Stop stop;

  @BeforeEach
  public void setUp() {
    stop = TransitModelForTest.stopForTest("name", "desc", 50, 10);
  }

  @Test
  public void digitransitVehicleParkingPropertyMapperTest() {
    var deduplicator = new Deduplicator();
    var stopModel = new StopModel();
    var graph = new Graph(stopModel, deduplicator);
    var transitModel = new TransitModel(stopModel, deduplicator);
    transitModel.index();

    DigitransitStopPropertyMapper mapper = DigitransitStopPropertyMapper.create(transitModel);

    Map<String, Object> map = new HashMap<>();
    mapper
      .map(
        new TransitStopVertexBuilder()
          .withGraph(graph)
          .withStop(stop)
          .withTransitModel(transitModel)
          .build()
      )
      .forEach(o -> map.put(o.first, o.second));

    assertEquals("F:name", map.get("gtfsId"));
    assertEquals("name", map.get("name"));
    assertEquals("desc", map.get("desc"));
  }
}
