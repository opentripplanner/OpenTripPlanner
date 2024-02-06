package org.opentripplanner.ext.geocoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.transit.model._data.TransitModelForTest.id;
import static org.opentripplanner.transit.model.basic.TransitMode.BUS;
import static org.opentripplanner.transit.model.basic.TransitMode.FERRY;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitModel;

class LuceneIndexTest {

  private static final TransitModelForTest TEST_MODEL = TransitModelForTest.of();

  static final Agency BVG = Agency
    .of(id("bvg"))
    .withName("BVG")
    .withTimezone("Europe/Berlin")
    .build();

  // Berlin
  static final Station BERLIN_HAUPTBAHNHOF_STATION = TEST_MODEL
    .station("Hauptbahnhof")
    .withCoordinate(52.52495, 13.36952)
    .build();
  static final Station ALEXANDERPLATZ_STATION = TEST_MODEL
    .station("Alexanderplatz")
    .withCoordinate(52.52277, 13.41046)
    .build();

  static final RegularStop ALEXANDERPLATZ_BUS = TEST_MODEL
    .stop("Alexanderplatz Bus")
    .withCoordinate(52.52277, 13.41046)
    .withVehicleType(BUS)
    .withParentStation(ALEXANDERPLATZ_STATION)
    .build();

  static final RegularStop ALEXANDERPLATZ_RAIL = TEST_MODEL
    .stop("Alexanderplatz S-Bahn")
    .withCoordinate(52.52157, 13.41123)
    .withVehicleType(TransitMode.RAIL)
    .withParentStation(ALEXANDERPLATZ_STATION)
    .build();
  static final RegularStop LICHTERFELDE_OST_1 = TEST_MODEL
    .stop("Lichterfelde Ost")
    .withId(id("lichterfelde-gleis-1"))
    .withCoordinate(52.42986, 13.32808)
    .build();
  static final RegularStop LICHTERFELDE_OST_2 = TEST_MODEL
    .stop("Lichterfelde Ost")
    .withId(id("lichterfelde-gleis-2"))
    .withCoordinate(52.42985, 13.32807)
    .build();
  static final RegularStop WESTHAFEN = TEST_MODEL
    .stop("Westhafen")
    .withVehicleType(null)
    .withCoordinate(52.42985, 13.32807)
    .build();

  // Atlanta
  static final Station FIVE_POINTS_STATION = TEST_MODEL
    .station("Five Points")
    .withCoordinate(33.753899, -84.39156)
    .build();

  static final RegularStop ARTS_CENTER = TEST_MODEL
    .stop("Arts Center")
    .withCode("4456")
    .withCoordinate(52.52277, 13.41046)
    .build();
  static final RegularStop ARTHUR = TEST_MODEL
    .stop("Arthur Langford Jr Pl SW at 220")
    .withCoordinate(52.52277, 13.41046)
    .build();

  static LuceneIndex index;

  static StopClusterMapper mapper;

  @BeforeAll
  static void setup() {
    var stopModel = TEST_MODEL.stopModelBuilder();
    List
      .of(
        ALEXANDERPLATZ_BUS,
        ALEXANDERPLATZ_RAIL,
        LICHTERFELDE_OST_1,
        LICHTERFELDE_OST_2,
        WESTHAFEN,
        ARTS_CENTER,
        ARTHUR
      )
      .forEach(stopModel::withRegularStop);
    List
      .of(ALEXANDERPLATZ_STATION, BERLIN_HAUPTBAHNHOF_STATION, FIVE_POINTS_STATION)
      .forEach(stopModel::withStation);
    var transitModel = new TransitModel(stopModel.build(), new Deduplicator());
    transitModel.index();
    var transitService = new DefaultTransitService(transitModel) {
      private final Multimap<StopLocation, TransitMode> modes = ImmutableMultimap
        .<StopLocation, TransitMode>builder()
        .putAll(WESTHAFEN, FERRY, BUS)
        .build();

      @Override
      public List<TransitMode> getModesOfStopLocation(StopLocation stop) {
        if (stop.getGtfsVehicleType() != null) {
          return List.of(stop.getGtfsVehicleType());
        } else {
          return List.copyOf(modes.get(stop));
        }
      }

      @Override
      public Agency getAgencyForId(FeedScopedId id) {
        if (id.equals(BVG.getId())) {
          return BVG;
        }
        return null;
      }

      @Override
      public Set<Route> getRoutesForStop(StopLocation stop) {
        return Set.of(TransitModelForTest.route("route1").withAgency(BVG).build());
      }

      @Override
      public FeedInfo getFeedInfo(String feedId) {
        return new FeedInfo(
          "F",
          "A Publisher",
          "http://example.com",
          "de",
          LocalDate.MIN,
          LocalDate.MIN,
          "1"
        );
      }
    };
    index = new LuceneIndex(transitService);
    mapper = new StopClusterMapper(transitService);
  }

  @Test
  void stopLocations() {
    var result1 = index.queryStopLocations("lich", true).toList();
    assertEquals(1, result1.size());
    assertEquals(LICHTERFELDE_OST_1.getName().toString(), result1.getFirst().getName().toString());

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

    @ParameterizedTest
    @ValueSource(
      strings = {
        "Alexanderplatz",
        "Alexa",
        "alex",
        "aleyanderplazt",
        "alexnderplazt",
        "Alexnderplatz",
        "Alexnaderplatz",
        "xande",
        "xanderpla",
        "alexnaderplaz",
        "Alexanderplat",
        "alexanderplat",
        "alexand",
        "alexander platz",
        "alexander-platz",
        "alexander",
      }
    )
    void stopClustersWithTypos(String searchTerm) {
      var results = index.queryStopClusters(searchTerm).toList();
      var ids = results.stream().map(StopCluster::id).toList();
      assertEquals(List.of(ALEXANDERPLATZ_STATION.getId()), ids);
    }

    @Test
    void fuzzyStopClusters() {
      var result1 = index.queryStopClusters("arts").map(StopCluster::id).toList();
      assertEquals(List.of(ARTS_CENTER.getId()), result1);
    }

    @Test
    void deduplicatedStopClusters() {
      var result = index.queryStopClusters("lich").toList();
      assertEquals(1, result.size());
      assertEquals(LICHTERFELDE_OST_1.getName().toString(), result.getFirst().name());
    }

    @ParameterizedTest
    @ValueSource(
      strings = {
        "five",
        "five ",
        "five p",
        "five po",
        "five poi",
        "five poin",
        "five point",
        "five points",
        "fife point",
        "five poits",
        "fife",
        "points",
        "ife points",
        "the five points",
        "five @ points",
        "five @ the points",
        "five@points",
        "five at points",
        "five&points",
        "five & points",
        "five and the points",
        "points five",
        "points fife",
      }
    )
    void stopClustersWithSpace(String query) {
      var result = index.queryStopClusters(query).map(StopCluster::id).toList();
      assertEquals(List.of(FIVE_POINTS_STATION.getId()), result);
    }

    @ParameterizedTest
    @ValueSource(strings = { "4456", "445" })
    void fuzzyStopCode(String query) {
      var result = index.queryStopClusters(query).toList();
      assertEquals(1, result.size());
      assertEquals(ARTS_CENTER.getName().toString(), result.getFirst().name());
    }

    @Test
    void modes() {
      var result = index.queryStopClusters("westh").toList();
      assertEquals(1, result.size());
      var stop = result.getFirst();
      assertEquals(WESTHAFEN.getName().toString(), stop.name());
      assertEquals(List.of(FERRY.name(), BUS.name()), stop.modes());
    }

    @Test
    void agenciesAndFeedPublisher() {
      var result = index.queryStopClusters("alexanderplatz").toList().getFirst();
      assertEquals(ALEXANDERPLATZ_STATION.getName().toString(), result.name());
      assertEquals(List.of(StopClusterMapper.toAgency(BVG)), result.agencies());
      assertEquals("A Publisher", result.feedPublisher().name());
    }
  }
}
