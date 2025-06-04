package org.opentripplanner.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.geojson.LngLatAlt;
import org.geojson.Polygon;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Location;
import org.onebusaway.gtfs.model.LocationGroup;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner._support.geometry.Polygons;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.model.site.GroupStop;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.transit.service.SiteRepositoryBuilder;

public class StopTimeMapperTest {

  private static final String FEED_ID = "FEED";
  private static final IdFactory ID_FACTORY = new IdFactory(FEED_ID);

  private static final AgencyAndId AGENCY_AND_ID = new AgencyAndId("A", "1");

  private static final Integer ID = 45;

  private static final int ARRIVAL_TIME = 1000;

  private static final int DEPARTURE_TIME = 2000;

  private static final int DROP_OFF_TYPE = 2;

  private static final int PICKUP_TYPE = 3;

  private static final String ROUTE_SHORT_NAME = "Route Short Name";

  private static final double SHAPE_DIST_TRAVELED = 2.5d;

  private static final String STOP_NAME = "Stop";

  private static final String HEAD_SIGN = "Head Sign";

  private static final int STOP_SEQUENCE = 4;

  private static final int TIMEPOINT = 50;

  private static final Trip TRIP = new GtfsTestData().trip;

  public static final DataImportIssueStore ISSUE_STORE = DataImportIssueStore.NOOP;
  private static final List<LngLatAlt> ZONE_COORDINATES = Arrays.stream(
    Polygons.BERLIN.getCoordinates()
  )
    .map(c -> new LngLatAlt(c.x, c.y))
    .toList();

  private final SiteRepositoryBuilder siteRepositoryBuilder = SiteRepository.of();

  private final StopMapper stopMapper = new StopMapper(
    ID_FACTORY,
    new TranslationHelper(),
    stationId -> null,
    siteRepositoryBuilder
  );
  private final BookingRuleMapper bookingRuleMapper = new BookingRuleMapper();
  private final LocationMapper locationMapper = new LocationMapper(
    ID_FACTORY,
    siteRepositoryBuilder,
    DataImportIssueStore.NOOP
  );
  private final LocationGroupMapper locationGroupMapper = new LocationGroupMapper(
    ID_FACTORY,
    stopMapper,
    locationMapper,
    siteRepositoryBuilder
  );
  private final TranslationHelper translationHelper = new TranslationHelper();
  private final StopTimeMapper subject = new StopTimeMapper(
    stopMapper,
    locationMapper,
    locationGroupMapper,
    new TripMapper(
      ID_FACTORY,
      new RouteMapper(ID_FACTORY, new AgencyMapper(ID_FACTORY), ISSUE_STORE, translationHelper),
      new DirectionMapper(ISSUE_STORE),
      translationHelper
    ),
    bookingRuleMapper,
    new TranslationHelper()
  );

  /**
   * Build a static ("regular") stop.
   */
  private static Stop buildStop() {
    var stop = new Stop();
    stop.setId(AGENCY_AND_ID);
    stop.setName(STOP_NAME);
    stop.setLat(53.12);
    stop.setLon(12.34);
    return stop;
  }

  /**
   * Builds a stop time with a fixed stop.
   */
  static StopTime buildDefaultStopTime() {
    var stopTime = buildStopTime();
    var stop = buildStop();
    stopTime.setStop(stop);
    return stopTime;
  }

  /**
   * Builds a stop time without a stop. Useful for testing flex fields.
   */
  private static StopTime buildStopTime() {
    TRIP.setId(AGENCY_AND_ID);

    var stopTime = new StopTime();
    stopTime.setId(ID);
    stopTime.setArrivalTime(ARRIVAL_TIME);
    stopTime.setDepartureTime(DEPARTURE_TIME);
    stopTime.setDropOffType(DROP_OFF_TYPE);
    stopTime.setPickupType(PICKUP_TYPE);
    stopTime.setRouteShortName(ROUTE_SHORT_NAME);
    stopTime.setShapeDistTraveled(SHAPE_DIST_TRAVELED);
    stopTime.setStopHeadsign(HEAD_SIGN);
    stopTime.setStopSequence(STOP_SEQUENCE);
    stopTime.setTimepoint(TIMEPOINT);
    stopTime.setTrip(TRIP);
    return stopTime;
  }

  @Test
  public void testMapCollection() {
    assertNull(subject.map((Collection<StopTime>) null));
    assertTrue(subject.map(Collections.emptyList()).isEmpty());
    assertEquals(1, subject.map(Collections.singleton(buildDefaultStopTime())).size());
  }

  @Test
  public void testMap() {
    var result = subject.map(buildDefaultStopTime());

    assertEquals(ARRIVAL_TIME, result.getArrivalTime());
    assertEquals(DEPARTURE_TIME, result.getDepartureTime());
    assertEquals(PickDrop.CALL_AGENCY, result.getDropOffType());
    assertEquals(PickDrop.COORDINATE_WITH_DRIVER, result.getPickupType());
    assertEquals(ROUTE_SHORT_NAME, result.getRouteShortName());
    assertEquals(SHAPE_DIST_TRAVELED, result.getShapeDistTraveled(), 0.0001d);
    assertNotNull(result.getStop());
    assertEquals(HEAD_SIGN, result.getStopHeadsign().toString());
    assertEquals(STOP_SEQUENCE, result.getStopSequence());
    assertEquals(TIMEPOINT, result.getTimepoint());
    assertNotNull(result.getTrip());
  }

  @Test
  public void testMapWithNulls() {
    var st = new StopTime();
    st.setStop(buildStop());
    var result = subject.map(st);

    assertFalse(result.isArrivalTimeSet());
    assertFalse(result.isDepartureTimeSet());
    assertEquals(PickDrop.SCHEDULED, result.getDropOffType());
    assertEquals(PickDrop.SCHEDULED, result.getPickupType());
    assertNull(result.getRouteShortName());
    assertFalse(result.isShapeDistTraveledSet());
    assertNotNull(result.getStop());
    assertNull(result.getStopHeadsign());
    assertEquals(0, result.getStopSequence());
    assertFalse(result.isTimepointSet());
  }

  /** Mapping the same object twice, should return the same instance. */
  @Test
  public void testMapCache() {
    var st = buildDefaultStopTime();
    var result1 = subject.map(st);
    var result2 = subject.map(st);
    assertSame(result1, result2);
  }

  @Test
  public void testNull() {
    var st = buildStopTime();
    Assertions.assertThrows(NullPointerException.class, () -> subject.map(st));
  }

  @Test
  public void testFlexLocation() {
    var st = buildStopTime();
    var flexLocation = new Location();
    flexLocation.setId(AGENCY_AND_ID);
    var polygon = new Polygon();
    polygon.setExteriorRing(ZONE_COORDINATES);
    flexLocation.setGeometry(polygon);
    st.setStop(flexLocation);
    var mapped = subject.map(st);

    assertInstanceOf(AreaStop.class, mapped.getStop());
    var areaStop = (AreaStop) mapped.getStop();
    assertEquals(Polygons.BERLIN, areaStop.getGeometry());
    assertEquals("FEED:1", areaStop.getId().toString());
  }

  @Test
  public void testFlexLocationGroup() {
    var st = buildStopTime();
    var locGroup = new LocationGroup();
    locGroup.setName("A location group");
    locGroup.setId(AGENCY_AND_ID);
    locGroup.addLocation(buildStop());
    st.setStop(locGroup);
    var mapped = subject.map(st);
    assertInstanceOf(GroupStop.class, mapped.getStop());

    var groupStop = (GroupStop) mapped.getStop();
    assertEquals("[RegularStop{FEED:1 Stop}]", groupStop.getChildLocations().toString());
  }
}
