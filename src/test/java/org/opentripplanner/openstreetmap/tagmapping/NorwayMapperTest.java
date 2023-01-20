package org.opentripplanner.openstreetmap.tagmapping;

import static java.lang.Double.NaN;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.openstreetmap.wayproperty.SafetyFeatures;
import org.opentripplanner.openstreetmap.wayproperty.WayPropertySet;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.test.support.VariableSource;

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

  static Stream<Arguments> createExpectedBicycleSafetyForMaxspeedCases() {
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
          var way = new OSMWithTags();
          way.addTag("highway", highway);
          way.addTag("maxspeed", String.valueOf(maxspeed));
          argumentsList.add(Arguments.of(way, expectedSafety));
        }
      }
    }
    return argumentsList.stream();
  }

  static Stream<Arguments> createBicycleSafetyWitoutExplisitMaxspeed() {
    Double[] expectedBicycleSafety = { 3.75, 3.75, 3.43, 3.43, 3.43, 1.83 };
    ArrayList<Arguments> argumentsList = new ArrayList<>();
    for (int i = 0; i < expectedHighways.length; i++) {
      var highway = expectedHighways[i];
      var expectedSafety = expectedBicycleSafety[i];
      var way = new OSMWithTags();
      way.addTag("highway", highway);
      argumentsList.add(Arguments.of(way, expectedSafety));
    }
    return argumentsList.stream();
  }

  static Stream<Arguments> createLinkRoadLikeMainCases() {
    ArrayList<Arguments> argumentsList = new ArrayList<>();
    for (var i = 0; i < 4; i++) {
      var highway = expectedHighways[i];
      for (var maxspeed : expectedMaxspeeds) {
        var mainRoad = new OSMWithTags();
        mainRoad.addTag("highway", highway);
        mainRoad.addTag("maxspeed", String.valueOf(maxspeed));
        var linkRoad = new OSMWithTags();
        linkRoad.addTag("highway", highway.concat("_link"));
        linkRoad.addTag("maxspeed", String.valueOf(maxspeed));
        argumentsList.add(Arguments.of(mainRoad, linkRoad));
      }
    }
    return argumentsList.stream();
  }

  static Stream<Arguments> expectedBicycleSafetyForMaxspeedCases = createExpectedBicycleSafetyForMaxspeedCases();
  static Stream<Arguments> expectedBicycleSafetyWitoutExplisitMaxspeed = createBicycleSafetyWitoutExplisitMaxspeed();
  static Stream<Arguments> linkRoadLikeMainCases = createLinkRoadLikeMainCases();

  @ParameterizedTest(name = "{0} should have a score of {1}")
  @VariableSource("expectedBicycleSafetyForMaxspeedCases")
  public void testBicycleSafetyForMaxspeed(OSMWithTags way, Double expected) {
    var result = wps.getDataForWay(way).getBicycleSafetyFeatures();
    var expectedSafetyFeatures = new SafetyFeatures(expected, expected);
    assertEquals(expectedSafetyFeatures, result);
  }

  @ParameterizedTest
  @VariableSource("expectedBicycleSafetyWitoutExplisitMaxspeed")
  public void testBicycleSafetyWithoutMaxspeed(OSMWithTags way, Double expected) {
    var result = wps.getDataForWay(way).getBicycleSafetyFeatures();
    var expectedSafetyFeatures = new SafetyFeatures(expected, expected);
    assertEquals(expectedSafetyFeatures, result);
  }

  @ParameterizedTest
  @VariableSource("linkRoadLikeMainCases")
  public void testBicycleSafetyLikeLinkRoad(OSMWithTags mainRoad, OSMWithTags linkRoad) {
    var resultMain = wps.getDataForWay(mainRoad).getBicycleSafetyFeatures();
    var resultLink = wps.getDataForWay(linkRoad).getBicycleSafetyFeatures();

    assertEquals(resultMain, resultLink);
  }

  @Test
  public void testTrunkIsWalkable() {
    var way = new OSMWithTags();
    way.addTag("highway", "trunk");

    assertEquals(StreetTraversalPermission.ALL, wps.getDataForWay(way).getPermission());
  }

  @Test
  public void testMtbScaleNone() {
    // https://www.openstreetmap.org/way/302610220
    var way1 = new OSMWithTags();
    way1.addTag("highway", "path");
    way1.addTag("mtb:scale", "3");

    assertEquals(StreetTraversalPermission.NONE, wps.getDataForWay(way1).getPermission());

    var way2 = new OSMWithTags();
    way2.addTag("highway", "track");
    way2.addTag("mtb:scale", "3");

    assertEquals(StreetTraversalPermission.NONE, wps.getDataForWay(way2).getPermission());
  }

  @Test
  public void testMtbScalePedestrian() {
    var way1 = new OSMWithTags();
    way1.addTag("highway", "path");
    way1.addTag("mtb:scale", "1");

    assertEquals(StreetTraversalPermission.PEDESTRIAN, wps.getDataForWay(way1).getPermission());

    var way2 = new OSMWithTags();
    way2.addTag("highway", "track");
    way2.addTag("mtb:scale", "1");

    assertEquals(StreetTraversalPermission.PEDESTRIAN, wps.getDataForWay(way2).getPermission());
  }

  @Test
  public void testMotorroad() {
    var way1 = new OSMWithTags();
    way1.addTag("highway", "trunk");
    way1.addTag("motorroad", "yes");

    assertEquals(StreetTraversalPermission.CAR, wps.getDataForWay(way1).getPermission());

    var way2 = new OSMWithTags();
    way2.addTag("highway", "primary");
    way2.addTag("motorroad", "yes");

    assertEquals(StreetTraversalPermission.CAR, wps.getDataForWay(way2).getPermission());
  }
}
