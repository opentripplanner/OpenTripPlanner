package org.opentripplanner.ext.vectortiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.vectortiles.layers.stops.DigitransitStopPropertyMapper;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.site.Stop;
import org.opentripplanner.transit.service.DefaultTransitService;
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
    var transitModel = new TransitModel(stopModel, deduplicator);
    transitModel.index();
    var transitService = new DefaultTransitService(transitModel);

    DigitransitStopPropertyMapper mapper = DigitransitStopPropertyMapper.create(transitService);

    Map<String, Object> map = new HashMap<>();
    mapper.map(stop).forEach(o -> map.put(o.first, o.second));

    assertEquals("F:name", map.get("gtfsId"));
    assertEquals("name", map.get("name"));
    assertEquals("desc", map.get("desc"));
  }
}
