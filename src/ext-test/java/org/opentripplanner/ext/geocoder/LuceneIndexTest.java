package org.opentripplanner.ext.geocoder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.StopModel;
import org.opentripplanner.transit.service.TransitModel;

class LuceneIndexTest {

  static Graph graph = new Graph();
  StopLocation ALEXANDERPLATZ_BUS = TransitModelForTest
    .stop("Alexanderplatz Bus")
    .withCoordinate(52.52277, 13.41046)
    .build();
  StopLocation ALEXANDERPLATZ_RAIL = TransitModelForTest
    .stop("Alexanderplatz S-Bahn")
    .withCoordinate(52.52157, 13.41123)
    .build();
  StopLocation LICHTERFELDE_OST = TransitModelForTest
    .stop("Lichterfelde Ost")
    .withCoordinate(52.42986, 13.32808)
    .build();

  @Test
  void deduplication() {
    var stopModel = StopModel.of();
    List
      .of(ALEXANDERPLATZ_BUS, ALEXANDERPLATZ_RAIL, LICHTERFELDE_OST)
      .forEach(sl -> stopModel.withRegularStop((RegularStop) sl));
    var transitModel = new TransitModel(stopModel.build(), new Deduplicator());
    transitModel.initTimeZone(ZoneIds.BERLIN);
    transitModel.index();
    var transitService = new DefaultTransitService(transitModel);
    var index = new LuceneIndex(graph, transitService);
    var result = index.queryStopLocations("lich", true).toList();
    assertEquals(List.of(LICHTERFELDE_OST), result);
  }
}
