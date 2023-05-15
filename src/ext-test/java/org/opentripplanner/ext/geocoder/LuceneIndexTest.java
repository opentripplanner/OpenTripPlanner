package org.opentripplanner.ext.geocoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.transit.model._data.TransitModelForTest.id;
import static org.opentripplanner.transit.model._data.TransitModelForTest.station;
import static org.opentripplanner.transit.model._data.TransitModelForTest.stop;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.StopModel;
import org.opentripplanner.transit.service.TransitModel;

class LuceneIndexTest {

  static Graph graph = new Graph();

  // Berlin
  static Station BERLIN_HAUPTBAHNHOF_STATION = station("Hauptbahnhof")
    .withCoordinate(52.52495, 13.36952)
    .build();
  static Station ALEXANDERPLATZ_STATION = station("Alexanderplatz")
    .withCoordinate(52.52277, 13.41046)
    .build();

  static RegularStop ALEXANDERPLATZ_BUS = stop("Alexanderplatz Bus")
    .withCoordinate(52.52277, 13.41046)
    .withParentStation(ALEXANDERPLATZ_STATION)
    .build();

  static RegularStop ALEXANDERPLATZ_RAIL = stop("Alexanderplatz S-Bahn")
    .withCoordinate(52.52157, 13.41123)
    .withParentStation(ALEXANDERPLATZ_STATION)
    .build();
  static RegularStop LICHTERFELDE_OST_1 = stop("Lichterfelde Ost")
    .withId(id("lichterfelde-gleis-1"))
    .withCoordinate(52.42986, 13.32808)
    .build();
  static RegularStop LICHTERFELDE_OST_2 = stop("Lichterfelde Ost")
    .withId(id("lichterfelde-gleis-2"))
    .withCoordinate(52.42985, 13.32807)
    .build();

  // Atlanta
  static Station FIVE_POINTS_STATION = station("Five Points")
    .withCoordinate(33.753899, -84.39156)
    .build();

  static RegularStop ARTS_CENTER = stop("Arts Center").withCoordinate(52.52277, 13.41046).build();
  static RegularStop ARTHUR = stop("Arthur Langford Jr Pl SW at 220")
    .withCoordinate(52.52277, 13.41046)
    .build();

  static LuceneIndex index;

  @BeforeAll
  static void setup() {
    var stopModel = StopModel.of();
    List
      .of(
        ALEXANDERPLATZ_BUS,
        ALEXANDERPLATZ_RAIL,
        LICHTERFELDE_OST_1,
        LICHTERFELDE_OST_2,
        ARTS_CENTER,
        ARTHUR
      )
      .forEach(stopModel::withRegularStop);
    List
      .of(ALEXANDERPLATZ_STATION, BERLIN_HAUPTBAHNHOF_STATION, FIVE_POINTS_STATION)
      .forEach(stopModel::withStation);
    var transitModel = new TransitModel(stopModel.build(), new Deduplicator());
    var transitService = new DefaultTransitService(transitModel);
    index = new LuceneIndex(graph, transitService);
  }

  @Test
  void stopLocations() {
    var result1 = index.queryStopLocations("lich", true).toList();
    assertEquals(1, result1.size());
    assertEquals(LICHTERFELDE_OST_1.getName().toString(), result1.get(0).getName().toString());

    var result2 = index.queryStopLocations("alexan", true).collect(Collectors.toSet());
    assertEquals(Set.of(ALEXANDERPLATZ_BUS, ALEXANDERPLATZ_RAIL), result2);
  }

  @Test
  void stopLocationGroups() {
    var result1 = index.queryStopLocationGroups("alex", true).toList();
    assertEquals(List.of(ALEXANDERPLATZ_STATION), result1);

    var result2 = index.queryStopLocationGroups("haupt", true).toList();
    assertEquals(List.of(BERLIN_HAUPTBAHNHOF_STATION), result2);
  }

  @Test
  void stopLocationGroupsWithSpace() {
    var result1 = index.queryStopLocationGroups("five points", true).toList();
    assertEquals(List.of(FIVE_POINTS_STATION), result1);
  }

  @Nested
  class StopClusters {

    @Test
    void stopClusters() {
      var result1 = index.queryStopClusters("alex").toList();
      assertEquals(List.of(StopCluster.of(ALEXANDERPLATZ_STATION)), result1);
    }

    @Test
    void fuzzyStopClusters() {
      var result1 = index.queryStopClusters("arts").toList();
      assertEquals(List.of(StopCluster.of(ARTS_CENTER).get()), result1);
    }

    @Test
    void deduplicatedStopClusters() {
      var result = index.queryStopClusters("lich").toList();
      assertEquals(1, result.size());
      assertEquals(LICHTERFELDE_OST_1.getName().toString(), result.get(0).name());
    }

    @ParameterizedTest
    @ValueSource(
      strings = {
        "five", "five ", "five p", "five po", "five poi", "five poin", "five point", "five points",
      }
    )
    void stopClustersWithSpace(String query) {
      var result = index.queryStopClusters(query).toList();
      assertEquals(List.of(StopCluster.of(FIVE_POINTS_STATION)), result);
    }
  }
}
