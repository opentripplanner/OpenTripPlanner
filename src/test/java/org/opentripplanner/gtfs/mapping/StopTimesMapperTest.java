package org.opentripplanner.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.model.PickDrop;

public class StopTimesMapperTest {

  private static final String FEED_ID = "FEED";

  private static final AgencyAndId AGENCY_AND_ID = new AgencyAndId("A", "1");

  private static final Integer ID = 45;

  private static final int ARRIVAL_TIME = 1000;

  private static final int DEPARTURE_TIME = 2000;

  private static final int DROP_OFF_TYPE = 2;

  private static final String FARE_PERIOD_ID = "Fare Period Id";

  private static final int PICKUP_TYPE = 3;

  private static final String ROUTE_SHORT_NAME = "Route Short Name";

  private static final double SHAPE_DIST_TRAVELED = 2.5d;

  private static final Stop STOP = new Stop();

  private static final String STOP_NAME = "Stop";

  private static final String HEAD_SIGN = "Head Sign";

  private static final int STOP_SEQUENCE = 4;

  private static final int TIMEPOINT = 50;

  private static final Trip TRIP = new GtfsTestData().trip;

  private static final StopTime STOP_TIME = new StopTime();

  public static final DataImportIssueStore ISSUE_STORE = DataImportIssueStore.NOOP;

  private final StopMapper stopMapper = new StopMapper(new TranslationHelper(), stationId -> null);
  private final BookingRuleMapper bookingRuleMapper = new BookingRuleMapper();
  private final LocationMapper locationMapper = new LocationMapper();
  private final LocationGroupMapper locationGroupMapper = new LocationGroupMapper(
    stopMapper,
    locationMapper
  );
  private final TranslationHelper translationHelper = new TranslationHelper();
  private final StopTimeMapper subject = new StopTimeMapper(
    stopMapper,
    locationMapper,
    locationGroupMapper,
    new TripMapper(
      new RouteMapper(new AgencyMapper(FEED_ID), ISSUE_STORE, translationHelper),
      new DirectionMapper(ISSUE_STORE),
      translationHelper
    ),
    bookingRuleMapper,
    new TranslationHelper()
  );

  static {
    TRIP.setId(AGENCY_AND_ID);
    STOP.setId(AGENCY_AND_ID);
    STOP.setName(STOP_NAME);

    STOP_TIME.setId(ID);
    STOP_TIME.setArrivalTime(ARRIVAL_TIME);
    STOP_TIME.setDepartureTime(DEPARTURE_TIME);
    STOP_TIME.setDropOffType(DROP_OFF_TYPE);
    STOP_TIME.setFarePeriodId(FARE_PERIOD_ID);
    STOP_TIME.setPickupType(PICKUP_TYPE);
    STOP_TIME.setRouteShortName(ROUTE_SHORT_NAME);
    STOP_TIME.setShapeDistTraveled(SHAPE_DIST_TRAVELED);
    STOP_TIME.setStop(STOP);
    STOP_TIME.setStopHeadsign(HEAD_SIGN);
    STOP_TIME.setStopSequence(STOP_SEQUENCE);
    STOP_TIME.setTimepoint(TIMEPOINT);
    STOP_TIME.setTrip(TRIP);
  }

  @Test
  public void testMapCollection() {
    assertNull(subject.map((Collection<StopTime>) null));
    assertTrue(subject.map(Collections.emptyList()).isEmpty());
    assertEquals(1, subject.map(Collections.singleton(STOP_TIME)).size());
  }

  @Test
  public void testMap() {
    org.opentripplanner.model.StopTime result = subject.map(STOP_TIME);

    assertEquals(ARRIVAL_TIME, result.getArrivalTime());
    assertEquals(DEPARTURE_TIME, result.getDepartureTime());
    assertEquals(PickDrop.CALL_AGENCY, result.getDropOffType());
    assertEquals(FARE_PERIOD_ID, result.getFarePeriodId());
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
    org.opentripplanner.model.StopTime result = subject.map(new StopTime());

    assertFalse(result.isArrivalTimeSet());
    assertFalse(result.isDepartureTimeSet());
    assertEquals(PickDrop.SCHEDULED, result.getDropOffType());
    assertNull(result.getFarePeriodId());
    assertEquals(PickDrop.SCHEDULED, result.getPickupType());
    assertNull(result.getRouteShortName());
    assertFalse(result.isShapeDistTraveledSet());
    assertNull(result.getStop());
    assertNull(result.getStopHeadsign());
    assertEquals(0, result.getStopSequence());
    assertFalse(result.isTimepointSet());
  }

  /** Mapping the same object twice, should return the the same instance. */
  @Test
  public void testMapCache() {
    org.opentripplanner.model.StopTime result1 = subject.map(STOP_TIME);
    org.opentripplanner.model.StopTime result2 = subject.map(STOP_TIME);

    assertSame(result1, result2);
  }
}
