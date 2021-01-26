package org.opentripplanner.routing.algorithm.raptor.transit.request;

import org.junit.Test;
import org.mockito.Mockito;
import org.opentripplanner.model.*;
import org.opentripplanner.routing.algorithm.raptor.transit.TripPatternForDate;
import org.opentripplanner.routing.algorithm.raptor.transit.TripPatternWithRaptorStopIndexes;
import org.opentripplanner.routing.trippattern.Deduplicator;
import org.opentripplanner.routing.trippattern.TripTimes;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RoutingRequestTransitDataProviderFilterTest {

  private static final FeedScopedId TEST_ROUTE_ID = new FeedScopedId("TEST", "ROUTE");

  private static final Stop STOP_FOR_TEST = Stop.stopForTest("TEST:STOP", 0, 0);

  @Test
  public void notFilteringExpectedTripPatternForDateTest() {
    TripPatternForDate tripPatternForDate = createTestTripPatternForDate();

    var filter = new RoutingRequestTransitDataProviderFilter(
        false,
        false,
        Set.of(TransitMode.BUS),
        Collections.emptySet()
    );

    boolean valid = filter.tripPatternPredicate(tripPatternForDate);

    assertTrue(valid);
  }

  private TripPatternForDate createTestTripPatternForDate() {
    Route route = new Route(TEST_ROUTE_ID);
    route.setMode(TransitMode.BUS);

    var stopTime = new StopTime();
    stopTime.setStop(STOP_FOR_TEST);
    StopPattern stopPattern = new StopPattern(List.of(stopTime));
    TripPattern pattern = new TripPattern(null, route, stopPattern);

    TripPatternWithRaptorStopIndexes tripPattern = new TripPatternWithRaptorStopIndexes(
        new int[0], pattern
    );

    TripTimes tripTimes = Mockito.mock(TripTimes.class);

    return new TripPatternForDate(tripPattern, new TripTimes[] {tripTimes}, LocalDate.now());
  }

  @Test
  public void bannedRouteFilteringTest() {
    TripPatternForDate tripPatternForDate = createTestTripPatternForDate();

    var filter = new RoutingRequestTransitDataProviderFilter(
        false,
        false,
        Set.of(TransitMode.BUS),
        Set.of(TEST_ROUTE_ID)
    );

    boolean valid = filter.tripPatternPredicate(tripPatternForDate);

    assertFalse(valid);
  }

  @Test
  public void transitModeFilteringTest() {
    TripPatternForDate tripPatternForDate = createTestTripPatternForDate();

    var filter = new RoutingRequestTransitDataProviderFilter(
        false,
        false,
        Collections.emptySet(),
        Collections.emptySet()
    );

    boolean valid = filter.tripPatternPredicate(tripPatternForDate);

    assertFalse(valid);
  }

  @Test
  public void notFilteringExpectedTripTimesTest() {
    TripTimes tripTimes = createTestTripTimes();

    var filter = new RoutingRequestTransitDataProviderFilter(
        false,
        false,
        Collections.emptySet(),
        Collections.emptySet()
    );

    boolean valid = filter.tripTimesPredicate(tripTimes);

    assertTrue(valid);
  }

  private TripTimes createTestTripTimes() {
    Trip trip = new Trip(new FeedScopedId("TEST", "TRIP"));
    trip.setBikesAllowed(2);

    StopTime stopTime = new StopTime();
    stopTime.setStop(STOP_FOR_TEST);
    stopTime.setArrivalTime(60);
    stopTime.setDepartureTime(60);
    stopTime.setStopSequence(0);

    return new TripTimes(trip, List.of(stopTime), new Deduplicator());
  }

  @Test
  public void bikesAllowedFilteringTest() {
    TripTimes tripTimes = createTestTripTimes();

    var filter = new RoutingRequestTransitDataProviderFilter(
        true,
        false,
        Collections.emptySet(),
        Collections.emptySet()
    );

    boolean valid = filter.tripTimesPredicate(tripTimes);

    assertFalse(valid);
  }

  @Test
  public void wheelchairAccessibleFilteringTest() {
    TripTimes tripTimes = createTestTripTimes();

    var filter = new RoutingRequestTransitDataProviderFilter(
        false,
        true,
        Collections.emptySet(),
        Collections.emptySet()
    );

    boolean valid = filter.tripTimesPredicate(tripTimes);

    assertFalse(valid);
  }

}
