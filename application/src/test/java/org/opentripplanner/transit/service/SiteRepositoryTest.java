package org.opentripplanner.transit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.model.site.GroupOfStations;
import org.opentripplanner.transit.model.site.GroupStop;
import org.opentripplanner.transit.model.site.MultiModalStation;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;

class SiteRepositoryTest {

  private static final WgsCoordinate COOR_A = new WgsCoordinate(60.0, 11.0);
  private static final WgsCoordinate COOR_B = new WgsCoordinate(62.0, 12.0);
  private static final Geometry GEOMETRY = GeometryUtils.getGeometryFactory()
    .createPoint(COOR_A.asJtsCoordinate());
  public static final NonLocalizedString NAME = NonLocalizedString.ofNullable("Name");
  private static final FeedScopedId ID = TimetableRepositoryForTest.id("A");
  private static final Station STATION = Station.of(ID)
    .withName(NAME)
    .withCoordinate(COOR_B)
    .build();
  private static final String EXP_STATIONS = List.of(STATION).toString();

  private final SiteRepositoryBuilder siteRepositoryBuilder = SiteRepository.of();
  private final RegularStop stop = siteRepositoryBuilder
    .regularStop(ID)
    .withCoordinate(COOR_A)
    .withName(NAME)
    .withParentStation(STATION)
    .build();
  private final String expStops = List.of(stop).toString();
  private final AreaStop STOP_AREA = siteRepositoryBuilder
    .areaStop(ID)
    .withName(NAME)
    .withGeometry(GEOMETRY)
    .build();
  private final GroupStop stopGroup = siteRepositoryBuilder.groupStop(ID).addLocation(stop).build();
  private final MultiModalStation mmStation = MultiModalStation.of(ID)
    .withName(NAME)
    .withChildStations(List.of(STATION))
    .withCoordinate(COOR_B)
    .build();
  private final String expMmStations = List.of(mmStation).toString();
  private final GroupOfStations groupOfStations = GroupOfStations.of(ID)
    .withName(NAME)
    .withCoordinate(COOR_B)
    .addChildStation(STATION)
    .build();
  private final String expGroupOfStation = List.of(groupOfStations).toString();

  @Test
  void testStop() {
    var m = siteRepositoryBuilder.withRegularStop(stop).build();
    assertEquals(stop, m.getRegularStop(ID));
    assertEquals(stop, m.getStopLocation(ID));
    assertEquals(expStops, m.listRegularStops().toString());
    assertEquals(expStops, m.listStopLocations().toString());
    assertEquals(stop, m.stopByIndex(stop.getIndex()));
    assertEquals(COOR_A, m.getCoordinateById(ID));
    assertFalse(m.hasAreaStops());
  }

  @Test
  void testAreaStop() {
    var m = siteRepositoryBuilder.withAreaStop(STOP_AREA).build();
    assertEquals(STOP_AREA, m.getAreaStop(ID));
    assertEquals(STOP_AREA, m.getStopLocation(ID));
    assertEquals("[AreaStop{F:A Name}]", m.listAreaStops().toString());
    assertEquals("[AreaStop{F:A Name}]", m.listStopLocations().toString());
    assertEquals(STOP_AREA, m.stopByIndex(STOP_AREA.getIndex()));
    assertEquals(COOR_A, m.getCoordinateById(ID));
    assertTrue(m.hasAreaStops());
  }

  @Test
  void testStopGroup() {
    var m = siteRepositoryBuilder.withGroupStop(stopGroup).build();
    assertEquals("[GroupStop{F:A}]", m.listGroupStops().toString());
    assertEquals("[GroupStop{F:A}]", m.listStopLocations().toString());
    assertEquals(stopGroup, m.stopByIndex(stopGroup.getIndex()));
    assertEquals(COOR_A, m.getCoordinateById(ID));
    assertFalse(m.hasAreaStops());
  }

  @Test
  void testStations() {
    var m = siteRepositoryBuilder.withStation(STATION).build();
    assertEquals(STATION, m.getStationById(ID));
    assertEquals(EXP_STATIONS, m.listStations().toString());
    assertEquals(STATION, m.getStopLocationsGroup(ID));
    assertEquals(expStops, m.findStopOrChildStops(ID).toString());
    assertEquals(EXP_STATIONS, m.listStopLocationGroups().toString());
    assertEquals(COOR_B, m.getCoordinateById(ID));
    assertFalse(m.hasAreaStops());
  }

  @Test
  void testMultiModalStation() {
    var m = siteRepositoryBuilder.withMultiModalStation(mmStation).build();
    assertEquals(mmStation, m.getMultiModalStation(ID));
    assertEquals(mmStation, m.getMultiModalStationForStation(STATION));
    assertEquals(expMmStations, m.listMultiModalStations().toString());
    assertEquals(mmStation, m.getStopLocationsGroup(ID));
    assertEquals(expStops, m.findStopOrChildStops(ID).toString());
    assertEquals(expMmStations, m.listStopLocationGroups().toString());
    assertEquals(COOR_B, m.getCoordinateById(ID));
    assertFalse(m.hasAreaStops());
  }

  @Test
  void testGroupOfStations() {
    var m = siteRepositoryBuilder.withGroupOfStation(groupOfStations).build();
    assertEquals(expGroupOfStation, m.listGroupOfStations().toString());
    assertEquals(groupOfStations, m.getStopLocationsGroup(ID));
    assertEquals(expStops, m.findStopOrChildStops(ID).toString());
    assertEquals(expGroupOfStation, m.listStopLocationGroups().toString());
    assertEquals(COOR_B, m.getCoordinateById(ID));
    assertFalse(m.hasAreaStops());
  }

  @Test
  void testNullStopLocationId() {
    var m = SiteRepository.of().build();
    assertNull(m.getStopLocation(null));
  }
}
