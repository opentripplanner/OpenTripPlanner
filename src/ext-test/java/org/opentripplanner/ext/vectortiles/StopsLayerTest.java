package org.opentripplanner.ext.vectortiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.ext.vectortiles.layers.stops.DigitransitStopPropertyMapper;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.opentripplanner.util.NonLocalizedString;

public class StopsLayerTest {

  private Stop stop;

  @Before
  public void setUp() {
    stop = Stop.stopForTest("name", "desc", 50, 10);
  }

  @Test
  public void digitransitVehicleParkingPropertyMapperTest() {
    Graph graph = mock(Graph.class);
    graph.index = mock(GraphIndex.class);

    DigitransitStopPropertyMapper mapper = DigitransitStopPropertyMapper.create(graph);
    Map<String, Object> map = new HashMap<>();
    mapper.map(new TransitStopVertex(graph, stop, null)).forEach(o -> map.put(o.first, o.second));

    assertEquals("F:name", map.get("gtfsId"));
    assertEquals("name", map.get("name"));
    assertEquals("desc", map.get("desc"));
  }
}
