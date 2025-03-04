package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import static org.opentripplanner.raptorlegacy._data.transit.TestTripSchedule.schedule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor.api.model.SearchDirection;
import org.opentripplanner.raptor.spi.RaptorTripScheduleSearch;
import org.opentripplanner.raptorlegacy._data.RaptorTestConstants;
import org.opentripplanner.raptorlegacy._data.transit.TestRoute;
import org.opentripplanner.raptorlegacy._data.transit.TestTripPattern;
import org.opentripplanner.raptorlegacy._data.transit.TestTripSchedule;

public class TripScheduleBoardSearchTest implements RaptorTestConstants {

  /*
   * To test board search we need a trip pattern, we will create
   * a trip pattern with 4 trips and 2 stops. This will cover most
   * of the simple cases:
   *
   * Trip:  |  A   |  B   |  C
   * Stop 1 | 1000 | 1200 | 2000
   * Stop 2 | 1500 | 1600 | 2500
   *
   * Note:
   * - All times are board times, we do not care about the alight times in this test.
   */

  /**
   * We use a relatively small prime number as the binary search threshold. This make it simpler to
   * construct a good test.
   */
  private static final int TRIPS_BINARY_SEARCH_THRESHOLD = 7;

  /** A time before all other times */
  private static final int TIME_0 = 0;

  private static final int TIME_A1 = 1000;
  private static final int TIME_A2 = 1500;

  private static final int TIME_B1 = 1200;
  private static final int TIME_B2 = 1600;

  private static final int TIME_C1 = 2000;
  private static final int TIME_C2 = 2500;

  private final TestTripPattern pattern = TestTripPattern.pattern("R1", STOP_A, STOP_B);

  private static final int TRIP_A = 0;
  private static final int TRIP_B = 1;
  private static final int TRIP_C = 2;

  // Route with trip A, B, C.
  private TestRoute route = TestRoute.route(pattern).withTimetable(
    schedule().departures(TIME_A1, TIME_A2),
    schedule().departures(TIME_B1, TIME_B2),
    schedule().departures(TIME_C1, TIME_C2)
  );

  // Trips in service
  private final TestTripSchedule tripA = route.timetable().getTripSchedule(TRIP_A);
  private final TestTripSchedule tripB = route.timetable().getTripSchedule(TRIP_B);

  // The service under test - the subject
  private RaptorTripScheduleSearch<TestTripSchedule> subject = route.tripSearch(
    SearchDirection.FORWARD
  );

  @Test
  public void noTripFoundAfterLastTripInServiceDeparture() {
    // When:
    //   Searching for a trip that board after the last trip i service (Trip C)
    // Then:
    //   No trips are expected as a result

    // Stop 1:
    searchForTrip(TIME_C1 + 1, STOP_POS_0).assertNoTripFound();

    // Stop 2:
    searchForTrip(TIME_C2 + 1, STOP_POS_1).assertNoTripFound();
  }

  @Test
  public void boardFirstTrip() {
    searchForTrip(TIME_0, STOP_POS_0).assertTripFound().withIndex(TRIP_A).withBoardTime(TIME_A1);

    searchForTrip(TIME_0, STOP_POS_1).assertTripFound().withIndex(TRIP_A).withBoardTime(TIME_A2);
  }

  @Test
  public void boardFirstTripWithTheMinimumPossibleSlack() {
    searchForTrip(TIME_A1, STOP_POS_0).assertTripFound().withIndex(TRIP_A).withBoardTime(TIME_A1);

    // Assert board next trip for: time + 1 second
    searchForTrip(TIME_A1 + 1, STOP_POS_0).assertTripFound().withIndex(TRIP_B);

    searchForTrip(TIME_A2, STOP_POS_1).assertTripFound().withIndex(TRIP_A).withBoardTime(TIME_A2);

    // Assert board next trip for: time + 1 second
    searchForTrip(TIME_A2 + 1, STOP_POS_1).assertTripFound().withIndex(TRIP_B);
  }

  @Test
  public void noTripsToBoardInEmptyPattern() {
    // The TripScheduleBoardSearch should handle an empty pattern without failing
    // and return no result found (false)
    withTrips(Collections.emptyList());
    searchForTrip(TIME_0, STOP_POS_0).assertNoTripFound();
  }

  @Test
  public void findTripWithGivenTripIndexUpperBound() {
    // Given a pattern with the following trips: A, B
    int TRIP_INDEX_A = 0;
    int TRIP_INDEX_B = 1;
    withTrips(tripA, tripB);

    // Then we expect to find trip A when `tripIndexUpperBound` is B´s index
    searchForTrip(TIME_0, STOP_POS_0, TRIP_INDEX_B)
      .assertTripFound()
      .withBoardTime(TIME_A1)
      .withIndex(TRIP_INDEX_A);

    // An then no trip if `tripIndexUpperBound` equals the first trip index (A´s index)
    searchForTrip(TIME_0, STOP_POS_1, TRIP_INDEX_A).assertNoTripFound();
  }

  @Test
  public void boardFirstAvailableTripForABigNumberOfTrips() {
    // For a pattern with N trip schedules,
    // where the first trip departure is at time 1000 and incremented by 1000.
    // We use 1 stop (we search for boardings, we do not care if we can alight)
    final int N = 7 * TRIPS_BINARY_SEARCH_THRESHOLD + 3;
    final int dT = 1000;

    List<TestTripSchedule> tripSchedules = new ArrayList<>();
    int departureTime = 1000;
    int latestDepartureTime = -1;

    for (int i = 0; i < N; ++i, departureTime += dT) {
      tripSchedules.add(schedule().departures(departureTime).build());
      latestDepartureTime = departureTime;
    }
    withTrips(tripSchedules);

    // Search for a trip that board after the last trip, expect no trip in return
    searchForTrip(latestDepartureTime + 1, STOP_POS_0).assertNoTripFound();

    for (int i = 0; i < N; ++i) {
      int tripBoardTime = dT * (i + 1);

      // Search and find trip 'i'
      searchForTrip(tripBoardTime, STOP_POS_0)
        .assertTripFound()
        .withIndex(i)
        .withBoardTime(tripBoardTime);

      // Search and find trip 'i' using the next trip index
      searchForTrip(tripBoardTime, STOP_POS_0, i + 1).assertTripFound().withIndex(i);

      // Search with a time and index that together exclude trip 'i'
      searchForTrip(tripBoardTime, STOP_POS_0, i).assertNoTripFound();
    }
  }

  private void withTrips(TestTripSchedule... schedules) {
    useTripPattern(TestRoute.route(pattern).withTimetable(schedules));
  }

  private void withTrips(List<TestTripSchedule> schedules) {
    withTrips(schedules.toArray(TestTripSchedule[]::new));
  }

  private void useTripPattern(TestRoute route) {
    this.route = route;
    this.subject = route.tripSearch(SearchDirection.FORWARD);
  }

  private TripAssert searchForTrip(int arrivalTime, int stopPosition) {
    return new TripAssert(subject).search(arrivalTime, stopPosition);
  }

  private TripAssert searchForTrip(int arrivalTime, int stopPosition, int tripIndexUpperBound) {
    return new TripAssert(subject).search(arrivalTime, stopPosition, tripIndexUpperBound);
  }
}
