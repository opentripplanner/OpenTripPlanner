package org.opentripplanner.updater;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import java.time.LocalDate;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.FeedScopedIdForTestFactory;
import org.opentripplanner.transit.model._data.TransitTestEnvironment;
import org.opentripplanner.transit.model._data.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model._data.TripInput;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.updater.trip.gtfs.GtfsRealtimeFuzzyTripMatcher;

public class GtfsRealtimeFuzzyTripMatcherTest {

  private static final String ROUTE_ID = "r1";
  private static final String FEED_ID = FeedScopedIdForTestFactory.FEED_ID;
  private static final LocalDate SERVICE_DATE = LocalDate.of(2024, 11, 13);
  private static final String GTFS_SERVICE_DATE = SERVICE_DATE.toString().replaceAll("-", "");
  private static final String TRIP_ID = "t1";
  private static final String START_TIME = "07:30:00";

  private final TransitTestEnvironmentBuilder builder = TransitTestEnvironment.of(SERVICE_DATE);
  private final RegularStop STOP_1 = builder.stop("s1");
  private final RegularStop STOP_2 = builder.stop("s2");
  private final Route ROUTE_A = builder.route(ROUTE_ID);

  private final TransitTestEnvironment env = builder
    .addTrip(
      TripInput.of(TRIP_ID).withRoute(ROUTE_A).addStop(STOP_1, START_TIME).addStop(STOP_2, "08:00")
    )
    .build();

  @Test
  void noTripId() {
    var matcher = matcher();
    TripDescriptor trip = matchingTripUpdate().build();
    assertEquals(TRIP_ID, matcher.match(FEED_ID, trip).getTripId());
  }

  @Test
  void tripIdSetButNotInSchedule() {
    var matcher = matcher();
    TripDescriptor trip = matchingTripUpdate().setTripId("does-not-exist-in-schedule").build();
    assertEquals(TRIP_ID, matcher.match(FEED_ID, trip).getTripId());
  }

  @Test
  void tripIdExistsInSchedule() {
    var matcher = matcher();
    TripDescriptor trip = matchingTripUpdate().setTripId(TRIP_ID).build();
    assertEquals(TRIP_ID, matcher.match(FEED_ID, trip).getTripId());
  }

  @Test
  void incorrectRoute() {
    var matcher = matcher();
    TripDescriptor trip = matchingTripUpdate().setRouteId("does-not-exists").build();
    assertFalse(matcher.match(FEED_ID, trip).hasTripId());
  }

  @Test
  void incorrectDateFormat() {
    var matcher = matcher();
    TripDescriptor trip = matchingTripUpdate().setStartDate("ZZZ").build();
    assertFalse(matcher.match(FEED_ID, trip).hasTripId());
  }

  @Test
  void incorrectDirection() {
    var matcher = matcher();
    TripDescriptor trip = matchingTripUpdate().setDirectionId(1).build();
    assertFalse(matcher.match(FEED_ID, trip).hasTripId());
  }

  @Test
  void noMatch() {
    // Test matching with "real time", when schedule uses time greater than 24:00
    var trip = TripDescriptor.newBuilder()
      .setRouteId("4")
      .setDirectionId(0)
      .setStartTime("12:00:00")
      .setStartDate("20090915")
      .build();
    // No departure at this time
    assertFalse(trip.hasTripId());
    trip = TripDescriptor.newBuilder()
      .setRouteId("1")
      .setStartTime("06:47:00")
      .setStartDate("20090915")
      .build();
    // Missing direction id
    assertFalse(trip.hasTripId());
  }

  @Nested
  class IncompleteData {

    @Test
    void noRouteId() {
      var td = matchingTripUpdate().clearRouteId().build();
      assertFalse(matcher().match(FEED_ID, td).hasTripId());
    }

    @Test
    void noDirectionId() {
      var td = matchingTripUpdate().clearDirectionId().build();
      assertFalse(matcher().match(FEED_ID, td).hasTripId());
    }

    @Test
    void noStartDate() {
      var td = matchingTripUpdate().clearStartDate().build();
      assertFalse(matcher().match(FEED_ID, td).hasTripId());
    }

    @Test
    void noStartTime() {
      var td = matchingTripUpdate().clearStartTime().build();
      assertFalse(matcher().match(FEED_ID, td).hasTripId());
    }
  }

  private GtfsRealtimeFuzzyTripMatcher matcher() {
    return new GtfsRealtimeFuzzyTripMatcher(env.transitService());
  }

  private static TripDescriptor.Builder matchingTripUpdate() {
    return TripDescriptor.newBuilder()
      .setRouteId(ROUTE_ID)
      .setDirectionId(2)
      .setStartTime(START_TIME)
      .setStartDate(GTFS_SERVICE_DATE);
  }
}
