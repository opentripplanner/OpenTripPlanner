package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import static org.opentripplanner.raptorlegacy._data.transit.TestTripPattern.pattern;
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

public class TripScheduleAlightSearchTest implements RaptorTestConstants {

  /*
   * To test alight search we need a trip pattern, we will create
   * a trip pattern with 4 trips and 2 stops. This will cover most
   * of the simple cases:
   *
   * Trip:  |  A   |  B   |  C
   * Stop 1 | 1000 | 2000 | 1900
   * Stop 2 | 1500 | 2500 | 2600
   *
   * Note:
   * - All times are alight times, we do not care about the board times in this test.
   */

  /**
   * We use a relatively small prime number as the binary search threshold. This make it simpler to
   * construct a good test.
   */
  private static final int TRIPS_BINARY_SEARCH_THRESHOLD = 7;

  private static final int TIME_A1 = 1000;
  private static final int TIME_A2 = 1500;

  private static final int TIME_B1 = 2000;
  private static final int TIME_B2 = 2500;

  private static final int TIME_C1 = 2100;
  private static final int TIME_C2 = 2600;

  /** A time after all other times */
  private static final int TIME_LATE = 9999;

  private static final int TRIP_A = 0;
  private static final int TRIP_B = 1;
  private static final int TRIP_C = 2;

  private final TestTripPattern pattern = pattern("R1", STOP_A, STOP_B);

  private TestRoute route = TestRoute.route(pattern).withTimetable(
    // Trips in service
    schedule().arrivals(TIME_A1, TIME_A2),
    schedule().arrivals(TIME_B1, TIME_B2),
    schedule().arrivals(TIME_C1, TIME_C2)
  );

  private final TestTripSchedule tripA = route.timetable().getTripSchedule(TRIP_A);
  private final TestTripSchedule tripB = route.timetable().getTripSchedule(TRIP_B);
  private final TestTripSchedule tripC = route.timetable().getTripSchedule(TRIP_C);

  // The service under test - the subject
  private RaptorTripScheduleSearch<TestTripSchedule> subject = route.tripSearch(
    SearchDirection.REVERSE
  );

  @Test
  public void noTripFoundBeforeFirstTrip() {
    // When:
    //   Searching for a trip that alight before the first trip (Trip A)
    // Then:
    //   No trips are expected as a result
    // Stop 1:
    searchForTrip(TIME_A1 - 1, STOP_POS_0).assertNoTripFound();

    // Stop 2:
    searchForTrip(TIME_A2 - 1, STOP_POS_1).assertNoTripFound();
  }

  @Test
  public void alightLastTripForAVeryLateTime() {
    searchForTrip(TIME_LATE, STOP_POS_0)
      .assertTripFound()
      .withIndex(TRIP_C)
      .withAlightTime(TIME_C1);

    searchForTrip(TIME_LATE, STOP_POS_1)
      .assertTripFound()
      .withIndex(TRIP_C)
      .withAlightTime(TIME_C2);
  }

  @Test
  public void findLastTripWithTheMinimumPossibleSlack() {
    // B matches B
    searchForTrip(TIME_B1, STOP_POS_0).assertTripFound().withIndex(TRIP_B).withAlightTime(TIME_B1);

    // One second minus, give the previous trip
    searchForTrip(TIME_B1 - 1, STOP_POS_0)
      .assertTripFound()
      .withIndex(TRIP_A)
      .withAlightTime(TIME_A1);
  }

  @Test
  public void noTripsToAlightInEmptyPattern() {
    // The TripScheduleAlightSearch should handle an empty pattern without failing
    // and return no result found (false)
    withTrips(Collections.emptyList());
    searchForTrip(TIME_LATE, STOP_POS_0).assertNoTripFound();
  }

  @Test
  public void findTripWithGivenTripIndexLowerBound() {
    // Given a pattern with the following trips: A, B
    withTrips(tripA, tripB);

    // Then we expect to find trip B when 'tripIndexLowerBound' is A´s index
    searchForTrip(TIME_LATE, STOP_POS_0, TRIP_A)
      .assertTripFound()
      .withAlightTime(TIME_B1)
      .withIndex(TRIP_B);

    // An then no trip if 'tripIndexLowerBound' equals the last trip index (B´s index)
    searchForTrip(TIME_LATE, STOP_POS_0, TRIP_B).assertNoTripFound();
  }

  @Test
  public void alightFirstAvailableTripForABigNumberOfTrips() {
    // For a pattern with N trip schedules,
    // where the first trip departure is at time 1000 and incremented by 1000.
    // We use 1 stop (we search for alighting, we do not care if we can board)
    final int firstArrivalTime = 1000;
    final int N = 7 * TRIPS_BINARY_SEARCH_THRESHOLD + 3;
    final int dT = 1000;

    List<TestTripSchedule> tripSchedules = new ArrayList<>();
    int arrivalTime = firstArrivalTime;

    for (int i = 0; i < N; ++i, arrivalTime += dT) {
      tripSchedules.add(schedule().arrivals(arrivalTime).build());
    }

    withTrips(tripSchedules);

    // Search for a trip that alight before the first trip, expect no trip in return
    searchForTrip(firstArrivalTime - 1, STOP_POS_0).assertNoTripFound();

    for (int i = 0; i < N; ++i) {
      int tripAlightTime = dT * (i + 1);

      // Search and find trip 'i'
      searchForTrip(tripAlightTime, STOP_POS_0)
        .assertTripFound()
        .withAlightTime(tripAlightTime)
        .withIndex(i);

      // Search and find trip 'i' using the previous trip index
      searchForTrip(tripAlightTime, STOP_POS_0, i - 1).assertTripFound().withIndex(i);

      // Search with a time and index that together exclude trip 'i'
      searchForTrip(tripAlightTime, STOP_POS_0, i).assertNoTripFound();
    }
  }

  /**
   * If there is a large number of trips not in service, the binary search may return a best guess
   * index which is above the correct trip index. This test make sure such trips are found.
   */
  @Test
  public void assertTripIsFoundEvenIfItIsBeforeTheBinarySearchUpperAndLowerBound() {
    // Given a pattern with N + 1 trip schedules
    List<TestTripSchedule> tripSchedules = new ArrayList<>();

    // Where the first trip is in service
    tripSchedules.add(tripA);
    final int indexA = 0;

    // And where the N next trips are NOT in service, but with acceptable boarding times
    addNTimes(tripSchedules, tripC, TRIPS_BINARY_SEARCH_THRESHOLD);

    withTrips(tripSchedules);

    // Then we expect to find A for both stop 1 and 2
    // Stop 1
    searchForTrip(TIME_A1, STOP_POS_0).assertTripFound().withIndex(indexA).withAlightTime(TIME_A1);

    // Stop 2
    searchForTrip(TIME_A2, STOP_POS_1).assertTripFound().withIndex(indexA).withAlightTime(TIME_A2);
  }

  private void withTrips(TestTripSchedule... schedules) {
    useRoute(TestRoute.route(pattern).withTimetable(schedules));
  }

  private void withTrips(List<TestTripSchedule> schedules) {
    withTrips(schedules.toArray(TestTripSchedule[]::new));
  }

  private void useRoute(TestRoute route) {
    this.route = route;
    this.subject = route.tripSearch(SearchDirection.REVERSE);
  }

  private static void addNTimes(List<TestTripSchedule> trips, TestTripSchedule tripS, int n) {
    for (int i = 0; i < n; i++) {
      trips.add(tripS);
    }
  }

  private TripAssert searchForTrip(int arrivalTime, int stopPosition) {
    return new TripAssert(subject).search(arrivalTime, stopPosition);
  }

  private TripAssert searchForTrip(int arrivalTime, int stopPosition, int tripIndexLowerBound) {
    return new TripAssert(subject).search(arrivalTime, stopPosition, tripIndexLowerBound);
  }
}
