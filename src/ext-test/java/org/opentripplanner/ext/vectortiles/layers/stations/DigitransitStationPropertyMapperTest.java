package org.opentripplanner.ext.vectortiles.layers.stations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.transit.model._data.TransitModelForTest.id;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.vectortiles.layers.TestTransitService;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.service.StopModel;
import org.opentripplanner.transit.service.TransitModel;

public class DigitransitStationPropertyMapperTest {

  @Test
  void map() {
    var deduplicator = new Deduplicator();
    var transitModel = new TransitModel(new StopModel(), deduplicator);
    transitModel.index();
    var transitService = new TestTransitService(transitModel);

    var mapper = DigitransitStationPropertyMapper.create(transitService, Locale.US);

    var station = Station
      .of(id("a-station"))
      .withCoordinate(1, 1)
      .withName(I18NString.of("A station"))
      .build();

    TransitModelForTest.of().stop("stop-1").withParentStation(station).build();

    Map<String, Object> map = new HashMap<>();
    mapper.map(station).forEach(o -> map.put(o.key(), o.value()));

    assertEquals("F:a-station", map.get("gtfsId"));
    assertEquals("A station", map.get("name"));
    assertEquals("", map.get("type"));
    assertEquals("[{\"mode\":\"RAIL\",\"shortName\":\"R1\"}]", map.get("routes"));
    assertEquals("[\"F:stop-1\"]", map.get("stops"));
  }
}
