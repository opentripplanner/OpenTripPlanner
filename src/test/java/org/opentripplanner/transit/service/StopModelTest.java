package org.opentripplanner.transit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.basic.NonLocalizedString;
import org.opentripplanner.transit.model.basic.WgsCoordinate;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.FlexLocationGroup;
import org.opentripplanner.transit.model.site.FlexStopLocation;
import org.opentripplanner.transit.model.site.GroupOfStations;
import org.opentripplanner.transit.model.site.MultiModalStation;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.Stop;
import org.opentripplanner.util.geometry.GeometryUtils;

class StopModelTest {

  private static final WgsCoordinate COOR_A = new WgsCoordinate(60.0, 11.0);
  private static final WgsCoordinate COOR_B = new WgsCoordinate(62.0, 12.0);
  private static final Geometry GEOMETRY = GeometryUtils
    .getGeometryFactory()
    .createPoint(COOR_A.asJtsCoordinate());
  public static final NonLocalizedString NAME = NonLocalizedString.ofNullable("Name");
  private static final FeedScopedId ID = TransitModelForTest.id("A");
  private static final Station STATION = Station
    .of(ID)
    .withName(NAME)
    .withCoordinate(COOR_B)
    .build();
  private static final String EXP_STATIONS = List.of(STATION).toString();

  private static final Stop STOP = Stop
    .of(ID)
    .withCoordinate(COOR_A)
    .withName(NAME)
    .withParentStation(STATION)
    .build();
  private static final String EXP_STOPS = List.of(STOP).toString();
  private static final FlexStopLocation STOP_AREA = FlexStopLocation
    .of(ID)
    .withName(NAME)
    .withGeometry(GEOMETRY)
    .build();
  private static final FlexLocationGroup STOP_GROUP = FlexLocationGroup
    .of(ID)
    .addLocation(STOP)
    .build();
  private static final MultiModalStation MM_STATION = MultiModalStation
    .of(ID)
    .withName(NAME)
    .withChildStations(List.of(STATION))
    .withCoordinate(COOR_B)
    .build();
  private static final String EXP_MM_STATIONS = List.of(MM_STATION).toString();
  private static final GroupOfStations GROUP_OF_STATIONS = GroupOfStations
    .of(ID)
    .withName(NAME)
    .withCoordinate(COOR_B)
    .addChildStation(STATION)
    .build();
  private static final String EXP_GROUP_OF_STATION = List.of(GROUP_OF_STATIONS).toString();

  @Test
  void testStop() {
    var m = StopModel.of().withStop(STOP).build();
    assertEquals(STOP, m.getRegularStop(ID));
    assertEquals(STOP, m.getStopLocation(ID));
    assertEquals(EXP_STOPS, m.listRegularStops().toString());
    assertEquals(EXP_STOPS, m.listStopLocations().toString());
    assertEquals(COOR_A, m.stopLocationCenter().orElseThrow());
    assertEquals(STOP, m.stopByIndex(STOP.getIndex()));
    assertEquals(COOR_A, m.getCoordinateById(ID));
    assertFalse(m.hasFlexLocations());
  }

  @Test
  void testStopArea() {
    var m = StopModel.of().withFlexStop(STOP_AREA).build();
    assertEquals(STOP_AREA, m.getFlexStopById(ID));
    assertEquals(STOP_AREA, m.getStopLocation(ID));
    assertEquals("[FlexStopLocation{F:A Name}]", m.getAllFlexLocations().toString());
    assertEquals("[FlexStopLocation{F:A Name}]", m.listStopLocations().toString());
    assertEquals(STOP_AREA, m.stopByIndex(STOP_AREA.getIndex()));
    assertEquals(COOR_A, m.stopLocationCenter().orElseThrow());
    assertEquals(COOR_A, m.getCoordinateById(ID));
    assertTrue(m.hasFlexLocations());
  }

  @Test
  void testStopGroup() {
    var m = StopModel.of().withFlexStopGroup(STOP_GROUP).build();
    assertEquals("[FlexLocationGroup{F:A}]", m.getAllFlexStopGroups().toString());
    assertEquals("[FlexLocationGroup{F:A}]", m.listStopLocations().toString());
    assertEquals(STOP_GROUP, m.stopByIndex(STOP_GROUP.getIndex()));
    assertEquals(COOR_A, m.stopLocationCenter().orElseThrow());
    assertEquals(COOR_A, m.getCoordinateById(ID));
    assertFalse(m.hasFlexLocations());
  }

  @Test
  void testStations() {
    var m = StopModel.of().withStation(STATION).build();
    assertEquals(STATION, m.getStationById(ID));
    assertEquals(EXP_STATIONS, m.getStations().toString());
    assertEquals(STATION, m.getStopLocationsGroup(ID));
    assertEquals(EXP_STOPS, m.getStopOrChildStops(ID).toString());
    assertEquals(EXP_STATIONS, m.listStopLocationGroups().toString());
    assertEquals(COOR_B, m.getCoordinateById(ID));
    assertTrue(m.stopLocationCenter().isEmpty());
    assertFalse(m.hasFlexLocations());
  }

  @Test
  void testMultiModalStation() {
    var m = StopModel.of().withMultiModalStation(MM_STATION).build();
    assertEquals(MM_STATION, m.getMultiModalStation(ID));
    assertEquals(MM_STATION, m.getMultiModalStationForStation(STATION));
    assertEquals(EXP_MM_STATIONS, m.getAllMultiModalStations().toString());
    assertEquals(MM_STATION, m.getStopLocationsGroup(ID));
    assertEquals(EXP_STOPS, m.getStopOrChildStops(ID).toString());
    assertEquals(EXP_MM_STATIONS, m.listStopLocationGroups().toString());
    assertEquals(COOR_B, m.getCoordinateById(ID));
    assertTrue(m.stopLocationCenter().isEmpty());
    assertFalse(m.hasFlexLocations());
  }

  @Test
  void testGroupOfStations() {
    var m = StopModel.of().withGroupOfStation(GROUP_OF_STATIONS).build();
    assertEquals(EXP_GROUP_OF_STATION, m.getAllGroupOfStations().toString());
    assertEquals(GROUP_OF_STATIONS, m.getStopLocationsGroup(ID));
    assertEquals(EXP_STOPS, m.getStopOrChildStops(ID).toString());
    assertEquals(EXP_GROUP_OF_STATION, m.listStopLocationGroups().toString());
    assertEquals(COOR_B, m.getCoordinateById(ID));
    assertTrue(m.stopLocationCenter().isEmpty());
    assertFalse(m.hasFlexLocations());
  }
}
