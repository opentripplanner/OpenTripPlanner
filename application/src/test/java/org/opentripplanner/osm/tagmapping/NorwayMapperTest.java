package org.opentripplanner.osm.tagmapping;

import static java.lang.Double.NaN;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.osm.WayTestData;
import org.opentripplanner.osm.model.OsmEntity;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.osm.wayproperty.WayPropertySet;
import org.opentripplanner.street.model.StreetTraversalPermission;

class NorwayMapperTest {

  private static final double EPSILON = 0.001;
  static final WayPropertySet WPS = new NorwayMapper().buildWayPropertySet();

  static int[] expectedMaxspeeds = { 90, 80, 70, 60, 50, 40, 30 };

  static String[] expectedHighways = {
    "trunk",
    "primary",
    "secondary",
    "tertiary",
    "unclassified",
    "residential",
  };

  static List<Arguments> createExpectedBicycleSafetyForMaxspeedCases() {
    Double[][] expectedBicycleSafetyMatrix = {
      { 10., 3.75, 3.75, 3.43, 2.5, 2.5, 1.83 },
      { 10., 3.75, 3.75, 3.43, 2.5, 2.5, 1.83 },
      { NaN, 3.43, 3.43, 2.5, 2.37, 2.37, 1.83 },
      { NaN, 3.43, 3.43, 2.37, 2.37, 1.83, 1.83 },
      { NaN, 3.43, 3.43, 1.83, 1.83, 1.83, 1.83 },
      { NaN, NaN, NaN, 1.83, 1.83, 1.83, 1.83 },
    };
    ArrayList<Arguments> argumentsList = new ArrayList<>();
    for (int i = 0; i < expectedHighways.length; i++) {
      var highway = expectedHighways[i];
      for (int j = 0; j < expectedMaxspeeds.length; j++) {
        var expectedSafety = expectedBicycleSafetyMatrix[i][j];
        if (!Double.isNaN(expectedSafety)) {
          var maxspeed = expectedMaxspeeds[j];
          var way = OsmWay.of();
          way.withTag("highway", highway);
          way.withTag("maxspeed", String.valueOf(maxspeed));
          argumentsList.add(Arguments.of(way.build(), expectedSafety));
        }
      }
    }
    return argumentsList;
  }

  static List<Arguments> createBicycleSafetyWithoutExplicitMaxspeed() {
    Double[] expectedBicycleSafety = { 3.75, 3.75, 3.43, 3.43, 3.43, 1.83 };
    ArrayList<Arguments> argumentsList = new ArrayList<>();
    for (int i = 0; i < expectedHighways.length; i++) {
      var highway = expectedHighways[i];
      var expectedSafety = expectedBicycleSafety[i];
      var way = OsmWay.of().withTag("highway", highway).build();
      argumentsList.add(Arguments.of(way, expectedSafety));
    }
    return argumentsList;
  }

  static List<Arguments> createLinkRoadLikeMainCases() {
    ArrayList<Arguments> argumentsList = new ArrayList<>();
    for (var i = 0; i < 4; i++) {
      var highway = expectedHighways[i];
      for (var maxspeed : expectedMaxspeeds) {
        var mainRoad = OsmWay.of();
        mainRoad.withTag("highway", highway);
        mainRoad.withTag("maxspeed", String.valueOf(maxspeed));
        var linkRoad = OsmWay.of();
        linkRoad.withTag("highway", highway.concat("_link"));
        linkRoad.withTag("maxspeed", String.valueOf(maxspeed));
        argumentsList.add(Arguments.of(mainRoad.build(), linkRoad.build()));
      }
    }
    return argumentsList;
  }

  @ParameterizedTest(name = "{0} should have a score of {1}")
  @MethodSource("createExpectedBicycleSafetyForMaxspeedCases")
  void testBicycleSafetyForMaxspeed(OsmEntity way, Double expected) {
    var result = WPS.getDataForEntity(way).bicycleSafety();
    assertEquals(expected, result);
  }

  @ParameterizedTest
  @MethodSource("createBicycleSafetyWithoutExplicitMaxspeed")
  void testBicycleSafetyWithoutMaxspeed(OsmEntity way, Double expected) {
    var result = WPS.getDataForEntity(way).bicycleSafety();
    assertEquals(expected, result);
  }

  @ParameterizedTest
  @MethodSource("createLinkRoadLikeMainCases")
  void testBicycleSafetyLikeLinkRoad(OsmEntity mainRoad, OsmEntity linkRoad) {
    var resultMain = WPS.getDataForEntity(mainRoad).bicycleSafety();
    var resultLink = WPS.getDataForEntity(linkRoad).bicycleSafety();

    assertEquals(resultMain, resultLink);
  }

  @Test
  void testTrunkIsWalkable() {
    var way = OsmWay.of().withTag("highway", "trunk").build();

    assertEquals(StreetTraversalPermission.ALL, WPS.getDataForEntity(way).getPermission());
  }

  @Test
  void testMtbScaleNone() {
    // https://www.openstreetmap.org/way/302610220
    var way1 = OsmWay.of().withTag("highway", "path").withTag("mtb:scale", "3").build();

    assertEquals(StreetTraversalPermission.NONE, WPS.getDataForEntity(way1).getPermission());

    var way2 = OsmWay.of().withTag("highway", "track").withTag("mtb:scale", "3").build();

    assertEquals(StreetTraversalPermission.NONE, WPS.getDataForEntity(way2).getPermission());
  }

  @Test
  void testMtbScalePedestrian() {
    var way1 = OsmWay.of().withTag("highway", "path").withTag("mtb:scale", "1").build();

    assertEquals(StreetTraversalPermission.PEDESTRIAN, WPS.getDataForEntity(way1).getPermission());

    var way2 = OsmWay.of().withTag("highway", "track").withTag("mtb:scale", "1").build();

    assertEquals(StreetTraversalPermission.PEDESTRIAN, WPS.getDataForEntity(way2).getPermission());
  }

  @Test
  void testMotorroad() {
    var way1 = OsmWay.of().withTag("highway", "trunk").withTag("motorroad", "yes").build();

    assertEquals(StreetTraversalPermission.CAR, WPS.getDataForEntity(way1).getPermission());

    var way2 = OsmWay.of().withTag("highway", "primary").withTag("motorroad", "yes").build();

    assertEquals(StreetTraversalPermission.CAR, WPS.getDataForEntity(way2).getPermission());
  }

  @Test
  void testFootway() {
    assertEquals(
      StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE,
      WPS.getDataForEntity(WayTestData.footway()).getPermission()
    );
  }

  @Test
  void testCycleway() {
    assertEquals(
      StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE,
      WPS.getDataForEntity(WayTestData.cycleway()).getPermission()
    );
  }

  @Test
  void testBridleway() {
    assertEquals(
      StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE,
      WPS.getDataForEntity(WayTestData.bridleway()).getPermission()
    );
  }

  @Test
  void embeddedRails() {
    assertEquals(6.86, WPS.getDataForEntity(WayTestData.embeddedRails()).bicycleSafety(), EPSILON);
  }
}
