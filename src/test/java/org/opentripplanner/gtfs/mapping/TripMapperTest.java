package org.opentripplanner.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.network.BikeAccess;
import org.opentripplanner.transit.model.network.CarAccess;
import org.opentripplanner.transit.model.timetable.Direction;

public class TripMapperTest {

  private static final String FEED_ID = "FEED";
  private static final AgencyAndId AGENCY_AND_ID = new AgencyAndId("A", "1");
  private static final int BIKES_ALLOWED = 1;
  private static final int CARS_ALLOWED = 1;
  private static final String BLOCK_ID = "Block Id";
  private static final int DIRECTION_ID = 1;
  private static final String TRIP_HEADSIGN = "Trip Headsign";
  private static final String TRIP_SHORT_NAME = "Trip Short Name";

  private static final int WHEELCHAIR_ACCESSIBLE = 1;

  private static final Trip TRIP = new Trip();

  public static final DataImportIssueStore ISSUE_STORE = DataImportIssueStore.NOOP;

  private final TripMapper subject = defaultTripMapper();

  private static TripMapper defaultTripMapper() {
    return new TripMapper(
      new RouteMapper(new AgencyMapper(FEED_ID), ISSUE_STORE, new TranslationHelper()),
      new DirectionMapper(ISSUE_STORE),
      new TranslationHelper()
    );
  }

  static {
    GtfsTestData data = new GtfsTestData();

    TRIP.setId(AGENCY_AND_ID);
    TRIP.setBikesAllowed(BIKES_ALLOWED);
    TRIP.setCarsAllowed(CARS_ALLOWED);
    TRIP.setBlockId(BLOCK_ID);
    TRIP.setDirectionId(Integer.toString(DIRECTION_ID));
    TRIP.setRoute(data.route);
    TRIP.setServiceId(AGENCY_AND_ID);
    TRIP.setShapeId(AGENCY_AND_ID);
    TRIP.setTripHeadsign(TRIP_HEADSIGN);
    TRIP.setTripShortName(TRIP_SHORT_NAME);
    TRIP.setWheelchairAccessible(WHEELCHAIR_ACCESSIBLE);
  }

  @Test
  void testMapCollection() throws Exception {
    assertNull(subject.map((Collection<Trip>) null));
    assertTrue(subject.map(Collections.emptyList()).isEmpty());
    assertEquals(1, subject.map(Collections.singleton(TRIP)).size());
  }

  @Test
  void testMap() throws Exception {
    org.opentripplanner.transit.model.timetable.Trip result = subject.map(TRIP);

    assertEquals("A:1", result.getId().toString());
    assertEquals(BLOCK_ID, result.getGtfsBlockId());
    assertEquals(Direction.INBOUND, result.getDirection());
    assertNotNull(result.getRoute());
    assertEquals("A:1", result.getServiceId().toString());
    assertEquals("A:1", result.getShapeId().toString());
    assertEquals(TRIP_HEADSIGN, result.getHeadsign().toString());
    assertEquals(TRIP_SHORT_NAME, result.getShortName());
    assertEquals(Accessibility.POSSIBLE, result.getWheelchairBoarding());
    assertEquals(BikeAccess.ALLOWED, result.getBikesAllowed());
    assertEquals(CarAccess.ALLOWED, result.getCarsAllowed());
  }

  @Test
  void testMapWithNulls() throws Exception {
    Trip input = new Trip();
    input.setId(AGENCY_AND_ID);
    input.setRoute(new GtfsTestData().route);

    org.opentripplanner.transit.model.timetable.Trip result = subject.map(input);

    assertNotNull(result.getId());
    assertNotNull(result.getRoute());

    assertNull(result.getGtfsBlockId());
    assertNull(result.getServiceId());
    assertNull(result.getShapeId());
    assertNull(result.getHeadsign());
    assertNull(result.getShortName());
    assertEquals(Direction.UNKNOWN, result.getDirection());
    assertEquals(Accessibility.NO_INFORMATION, result.getWheelchairBoarding());
    assertEquals(BikeAccess.UNKNOWN, result.getBikesAllowed());
    assertEquals(CarAccess.UNKNOWN, result.getCarsAllowed());
  }

  /** Mapping the same object twice, should return the same instance. */
  @Test
  void testMapCache() throws Exception {
    org.opentripplanner.transit.model.timetable.Trip result1 = subject.map(TRIP);
    org.opentripplanner.transit.model.timetable.Trip result2 = subject.map(TRIP);

    assertSame(result1, result2);
  }

  @Test
  void noFlexTimePenalty() {
    var mapper = defaultTripMapper();
    mapper.map(TRIP);
    assertTrue(mapper.flexSafeTimePenalties().isEmpty());
  }

  @Test
  void flexTimePenalty() {
    var flexTrip = new Trip();
    flexTrip.setId(new AgencyAndId("1", "1"));
    flexTrip.setSafeDurationFactor(1.5);
    flexTrip.setSafeDurationOffset(600d);
    flexTrip.setRoute(new GtfsTestData().route);
    var mapper = defaultTripMapper();
    var mapped = mapper.map(flexTrip);
    var penalty = mapper.flexSafeTimePenalties().get(mapped);
    assertEquals(1.5f, penalty.coefficient());
    assertEquals(600, penalty.constant().toSeconds());
  }
}
