package org.opentripplanner.transit.raptor.rangeraptor.transit;

import org.junit.Test;
import org.opentripplanner.transit.raptor._shared.TestRoute;
import org.opentripplanner.transit.raptor._shared.TestRaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.transit.RaptorRoute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class TripScheduleAlightSearchTest {

    /*
     * To test alight search we need a trip pattern, we will create
     * a trip pattern with 4 trips and 2 stops. This will cover most
     * of the simple cases:
     *
     * Trip:  |  A   |  B   |  C   |  D
     * Stop 1 | 1000 | 2000 | 1900 | 2100
     * Stop 2 | 1500 | 2500 | 2600 | 2400
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

    /* Stop position in pattern */
    private static final int STOP_1 = 0;
    private static final int STOP_2 = 1;

    private static final int TRIP_A_INDEX = 0;
    private static final int TRIP_B_INDEX = 1;
    private static final int TRIP_C_INDEX = 2;

    // Trips in service
    private TestRaptorTripSchedule tripA = TestRaptorTripSchedule
            .create("T-A")
            .withAlightTimes(TIME_A1, TIME_A2)
            .build();
    private TestRaptorTripSchedule tripB = TestRaptorTripSchedule
            .create("T-B")
            .withAlightTimes(TIME_B1, TIME_B2)
            .build();
    private TestRaptorTripSchedule tripC = TestRaptorTripSchedule
            .create("T-C")
            .withAlightTimes(TIME_C1, TIME_C2)
            .build();

    // Trip pattern with trip A and B.
    private RaptorRoute<TestRaptorTripSchedule> route = new TestRoute(tripA, tripB, tripC);

    // The service under test - the subject
    private TripScheduleAlightSearch<TestRaptorTripSchedule> subject = new TripScheduleAlightSearch<>(
            TRIPS_BINARY_SEARCH_THRESHOLD, route.timetable()
    );

    @Test
    public void noTripFoundBeforeFirstTrip() {
        // When:
        //   Searching for a trip that alight before the first trip (Trip A)
        // Then:
        //   No trips are expected as a result
        // Stop 1:
        searchForTrip(TIME_A1 - 1, STOP_1).assertNoTripFound();

        // Stop 2:
        searchForTrip(TIME_A2 - 1, STOP_2).assertNoTripFound();
    }

    @Test
    public void alightLastTripForAVeryLateTime() {
        searchForTrip(TIME_LATE, STOP_1)
                .assertTripFound()
                .withIndex(TRIP_C_INDEX)
                .withAlightTime(TIME_C1);

        searchForTrip(TIME_LATE, STOP_2)
                .assertTripFound()
                .withIndex(TRIP_C_INDEX)
                .withAlightTime(TIME_C2);
    }

    @Test
    public void findLastTripWithTheMinimumPossibleSlack() {
        // B matches B
        searchForTrip(TIME_B1, STOP_1)
                .assertTripFound()
                .withIndex(TRIP_B_INDEX)
                .withAlightTime(TIME_B1);

        // One second minus, give the previous trip
        searchForTrip(TIME_B1-1, STOP_1)
                .assertTripFound()
                .withIndex(TRIP_A_INDEX)
                .withAlightTime(TIME_A1);
    }

    @Test
    public void noTripsToAlightInEmptyPattern() {
        // The TripScheduleAlightSearch should handle an empty pattern without failing
        // and return no result found (false)
        withTrips(Collections.emptyList());
        searchForTrip(TIME_LATE, STOP_1)
                .assertNoTripFound();
    }

    @Test
    public void findTripWithGivenTripIndexLowerBound() {
        // Given a pattern with the following trips: A, B
        withTrips(tripA, tripB);

        // Then we expect to find trip B when 'tripIndexLowerBound' is A´s index
        searchForTrip(TIME_LATE, STOP_1, TRIP_A_INDEX)
                .assertTripFound()
                .withAlightTime(TIME_B1)
                .withIndex(TRIP_B_INDEX);

        // An then no trip if 'tripIndexLowerBound' equals the last trip index (B´s index)
        searchForTrip(TIME_LATE, STOP_1, TRIP_B_INDEX)
                .assertNoTripFound();
    }

    @Test
    public void alightFirstAvailableTripForABigNumberOfTrips() {
        // For a pattern with N trip schedules,
        // where the first trip departure is at time 1000 and incremented by 1000.
        // We use 1 stop (we search for alighting, we do not care if we can board)
        final int firstArrivalTime = 1000;
        final int n = TRIPS_BINARY_SEARCH_THRESHOLD;
        final int N = 7 * n + 3;
        final int dT = 1000;

        List<TestRaptorTripSchedule> tripSchedules = new ArrayList<>();
        int arrivalTime = firstArrivalTime;

        for (int i = 0; i < N; ++i, arrivalTime += dT) {
            tripSchedules.add(
                    TestRaptorTripSchedule.create("T-" + i+1).withAlightTimes(arrivalTime).build()
            );
        }
        useTripPattern(new TestRoute(tripSchedules));


        // Search for a trip that alight before the first trip, expect no trip in return
        searchForTrip(firstArrivalTime - 1, STOP_1)
                .assertNoTripFound();

        for (int i = 0; i < N; ++i) {
            int tripAlightTime = dT * (i + 1);
            int okSearchTime = tripAlightTime;

            // Search and find trip 'i'
            searchForTrip(okSearchTime, STOP_1)
                    .assertTripFound()
                    .withAlightTime(tripAlightTime)
                    .withIndex(i);

            // Search and find trip 'i' using the previous trip index
            searchForTrip(okSearchTime, STOP_1, i-1)
                    .assertTripFound()
                    .withIndex(i);

            // Search with a time and index that together exclude trip 'i'
            searchForTrip(tripAlightTime, STOP_1, i)
                    .assertNoTripFound();
        }
    }

    /**
     * If there is a large number of trips not in service, the binary search may return
     * a best guess index witch is above the correct trip index. This test make sure
     * such trips are found.
     */
    @Test
    public void assertTripIsFoundEvenIfItIsBeforeTheBinarySearchUpperAndLowerBound() {
        // Given a pattern with N + 1 trip schedules
        List<TestRaptorTripSchedule> tripSchedules = new ArrayList<>();

        // Where the first trip is in service
        tripSchedules.add(tripA);
        final int indexA = 0;

        // And where the N next trips are NOT in service, but with acceptable boarding times
        addNTimes(tripSchedules, tripC, TRIPS_BINARY_SEARCH_THRESHOLD);

        useTripPattern(new TestRoute(tripSchedules));

        // Then we expect to find A for both stop 1 and 2
        // Stop 1
        searchForTrip(TIME_A1, STOP_1)
                .assertTripFound()
                .withIndex(indexA)
                .withAlightTime(TIME_A1);

        // Stop 2
        searchForTrip(TIME_A2, STOP_2)
                .assertTripFound()
                .withIndex(indexA)
                .withAlightTime(TIME_A2);
    }

    private void withTrips(TestRaptorTripSchedule... schedules) {
        useTripPattern(new TestRoute(schedules));
    }

    private void withTrips(List<TestRaptorTripSchedule> schedules) {
        useTripPattern(new TestRoute(schedules));
    }

    private void useTripPattern(TestRoute pattern) {
        this.route = pattern;
        this.subject = new TripScheduleAlightSearch<>(
                TRIPS_BINARY_SEARCH_THRESHOLD,
                this.route.timetable()
        );
    }

    private static void addNTimes(List<TestRaptorTripSchedule> trips, TestRaptorTripSchedule tripS, int n) {
        for (int i = 0; i < n; i++) {
            trips.add(tripS);
        }
    }

    private TripAssert searchForTrip(int arrivalTime, int stopPosition) {
        return new TripAssert(subject)
                .search(arrivalTime, stopPosition);
    }

    private TripAssert searchForTrip(int arrivalTime, int stopPosition, int tripIndexLowerBound) {
        return new TripAssert(subject)
                .search(arrivalTime, stopPosition, tripIndexLowerBound);
    }
}
