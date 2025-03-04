package org.opentripplanner.osm.tagmapping;

import static java.lang.Double.NaN;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.osm.model.OsmEntity;
import org.opentripplanner.osm.wayproperty.SafetyFeatures;
import org.opentripplanner.osm.wayproperty.WayPropertySet;
import org.opentripplanner.street.model.StreetTraversalPermission;

public class NorwayMapperTest {

  static WayPropertySet wps = new WayPropertySet();

  static {
    var source = new NorwayMapper();
    source.populateProperties(wps);
  }

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
          var way = new OsmEntity();
          way.addTag("highway", highway);
          way.addTag("maxspeed", String.valueOf(maxspeed));
          argumentsList.add(Arguments.of(way, expectedSafety));
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
      var way = new OsmEntity();
      way.addTag("highway", highway);
      argumentsList.add(Arguments.of(way, expectedSafety));
    }
    return argumentsList;
  }

  static List<Arguments> createLinkRoadLikeMainCases() {
    ArrayList<Arguments> argumentsList = new ArrayList<>();
    for (var i = 0; i < 4; i++) {
      var highway = expectedHighways[i];
      for (var maxspeed : expectedMaxspeeds) {
        var mainRoad = new OsmEntity();
        mainRoad.addTag("highway", highway);
        mainRoad.addTag("maxspeed", String.valueOf(maxspeed));
        var linkRoad = new OsmEntity();
        linkRoad.addTag("highway", highway.concat("_link"));
        linkRoad.addTag("maxspeed", String.valueOf(maxspeed));
        argumentsList.add(Arguments.of(mainRoad, linkRoad));
      }
    }
    return argumentsList;
  }

  @ParameterizedTest(name = "{0} should have a score of {1}")
  @MethodSource("createExpectedBicycleSafetyForMaxspeedCases")
  public void testBicycleSafetyForMaxspeed(OsmEntity way, Double expected) {
    var result = wps.getDataForWay(way).bicycleSafety();
    var expectedSafetyFeatures = new SafetyFeatures(expected, expected);
    assertEquals(expectedSafetyFeatures, result);
  }

  @ParameterizedTest
  @MethodSource("createBicycleSafetyWithoutExplicitMaxspeed")
  public void testBicycleSafetyWithoutMaxspeed(OsmEntity way, Double expected) {
    var result = wps.getDataForWay(way).bicycleSafety();
    var expectedSafetyFeatures = new SafetyFeatures(expected, expected);
    assertEquals(expectedSafetyFeatures, result);
  }

  @ParameterizedTest
  @MethodSource("createLinkRoadLikeMainCases")
  public void testBicycleSafetyLikeLinkRoad(OsmEntity mainRoad, OsmEntity linkRoad) {
    var resultMain = wps.getDataForWay(mainRoad).bicycleSafety();
    var resultLink = wps.getDataForWay(linkRoad).bicycleSafety();

    assertEquals(resultMain, resultLink);
  }

  @Test
  public void testTrunkIsWalkable() {
    var way = new OsmEntity();
    way.addTag("highway", "trunk");

    assertEquals(StreetTraversalPermission.ALL, wps.getDataForWay(way).getPermission());
  }

  @Test
  public void testMtbScaleNone() {
    // https://www.openstreetmap.org/way/302610220
    var way1 = new OsmEntity();
    way1.addTag("highway", "path");
    way1.addTag("mtb:scale", "3");

    assertEquals(StreetTraversalPermission.NONE, wps.getDataForWay(way1).getPermission());

    var way2 = new OsmEntity();
    way2.addTag("highway", "track");
    way2.addTag("mtb:scale", "3");

    assertEquals(StreetTraversalPermission.NONE, wps.getDataForWay(way2).getPermission());
  }

  @Test
  public void testMtbScalePedestrian() {
    var way1 = new OsmEntity();
    way1.addTag("highway", "path");
    way1.addTag("mtb:scale", "1");

    assertEquals(StreetTraversalPermission.PEDESTRIAN, wps.getDataForWay(way1).getPermission());

    var way2 = new OsmEntity();
    way2.addTag("highway", "track");
    way2.addTag("mtb:scale", "1");

    assertEquals(StreetTraversalPermission.PEDESTRIAN, wps.getDataForWay(way2).getPermission());
  }

  @Test
  public void testMotorroad() {
    var way1 = new OsmEntity();
    way1.addTag("highway", "trunk");
    way1.addTag("motorroad", "yes");

    assertEquals(StreetTraversalPermission.CAR, wps.getDataForWay(way1).getPermission());

    var way2 = new OsmEntity();
    way2.addTag("highway", "primary");
    way2.addTag("motorroad", "yes");

    assertEquals(StreetTraversalPermission.CAR, wps.getDataForWay(way2).getPermission());
  }
}
