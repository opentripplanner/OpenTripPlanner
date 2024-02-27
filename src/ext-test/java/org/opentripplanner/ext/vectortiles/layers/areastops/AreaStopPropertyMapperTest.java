package org.opentripplanner.ext.vectortiles.layers.areastops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.inspector.vector.KeyValue.kv;

import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.geometry.Polygons;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.service.StopModel;

class AreaStopPropertyMapperTest {

  private static final TransitModelForTest MODEL = new TransitModelForTest(StopModel.of());
  private static final AreaStop STOP = MODEL.areaStopForTest("123", Polygons.BERLIN);
  private static final Route ROUTE_WITH_COLOR = TransitModelForTest
    .route("123")
    .withColor("ffffff")
    .build();
  private static final Route ROUTE_WITHOUT_COLOR = TransitModelForTest.route("456").build();

  @Test
  void map() {
    var mapper = new AreaStopPropertyMapper(
      ignored -> List.of(ROUTE_WITH_COLOR, ROUTE_WITHOUT_COLOR),
      Locale.ENGLISH
    );

    var kv = mapper.map(STOP);

    assertEquals(
      List.of(
        kv("gtfsId", "F:123"),
        kv("name", "123"),
        kv("code", null),
        kv("routeColors", "ffffff")
      ),
      kv
    );
  }
}
