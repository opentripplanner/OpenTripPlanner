package org.opentripplanner.ext.vectortiles.layers.areastops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.inspector.vector.KeyValue.kv;

import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.service.SiteRepository;

class AreaStopPropertyMapperTest {

  private static final TimetableRepositoryForTest MODEL = new TimetableRepositoryForTest(
    SiteRepository.of()
  );
  private static final AreaStop STOP = MODEL.areaStop("123").build();
  private static final Route ROUTE_WITH_COLOR = TimetableRepositoryForTest.route("123")
    .withColor("ffffff")
    .build();
  private static final Route ROUTE_WITHOUT_COLOR = TimetableRepositoryForTest.route("456").build();

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
