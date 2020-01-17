package org.opentripplanner.transit.raptor.rangeraptor.transit;

import org.junit.Test;
import org.opentripplanner.transit.raptor.api.TestTripPattern;
import org.opentripplanner.transit.raptor.api.TestRaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.transit.TripPatternInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.opentripplanner.transit.raptor.api.TestRaptorTripSchedule.createTripScheduleUseingArrivalTimes;


public class TripScheduleAlightSearchTest {

    /*
     * To test alight search we need a trip pattern, we will create
     * a trip pattern with 4 trips and 2 stops. This will cover most
     * of the simple cases:
     *
     * Trip:  |  S   |  A   |  T   |  B
     * Stop 1 |  900 | 1000 | 1100 | 2000
     * Stop 2 | 2500 | 1500 | 1400 | 2500
     *
     * Note:
     * - All times are alight times, we do not care about the board times in this test.
     * - Trip S and T is not in service
     * - Trip S depart from stop 1 before trip B, but depart from stop 2 after trip B.
     * - Trip T depart from stop 1 after trip B, but depart from stop 2 before trip B.
     *
     * The TripScheduleAlightSearch should handle the above trip variations. The point here
     * is that trip B, S and T is in order at stop 1, but not at stop 2.
     */

    /**
     * We use a relatively small prime number as the binary search threshold. This make it simpler to
     * construct a good test.
     */
    private static final int TRIPS_BINARY_SEARCH_THRESHOLD = 7;

    /** A time before all other times */
    private static final int TIME_LATE = 9999;

    private static final int TIME_A0 = 1000;
    private static final int TIME_A1 = 1500;

    private static final int TIME_B0 = 2000;
    private static final int TIME_B1 = 2500;

    private static final int TIME_S0 = 900;
    private static final int TIME_S1 = 1600;

    private static final int TIME_T0 = 1100;
    private static final int TIME_T1 = 1400;

    /* Stop position in pattern */
    private static final int STOP_1 = 0;
    private static final int STOP_2 = 1;

    private static final int TRIP_A_INDEX = 1;
    private static final int TRIP_B_INDEX = 3;

    // Trips in service
    private TestRaptorTripSchedule tripA = createTripScheduleUseingArrivalTimes(TIME_A0, TIME_A1);
    private TestRaptorTripSchedule tripB = createTripScheduleUseingArrivalTimes(TIME_B0, TIME_B1);

    // Trips not in service
    private TestRaptorTripSchedule tripS = createTripScheduleUseingArrivalTimes(TIME_S0, TIME_S1);
    private TestRaptorTripSchedule tripT = createTripScheduleUseingArrivalTimes(TIME_T0, TIME_T1);

    // Trip pattern with trip S, A, T, and B.
    private TripPatternInfo<TestRaptorTripSchedule> pattern = new TestTripPattern(tripS, tripA, tripT, tripB);

    // The service under test - the subject
    private TripScheduleAlightSearch<TestRaptorTripSchedule> subject = new TripScheduleAlightSearch<>(
            TRIPS_BINARY_SEARCH_THRESHOLD, pattern, this::skip
    );

    @Test
    public void noTripFoundBeforeFirstTripInServiceArrival() {
        // When:
        //   Searching for a trip that alight before the first trip i service (Trip A)
        // Then:
        //   No trips are expected as a result
        // Stop 1: (Trip S alight before A, but is not in service)
        searchForTrip(earliestTimeNotAlightingAt(TIME_A0), STOP_1)
                .assertNoTripFound();
        // Stop 2: (Trip T alight before A, but is not in service)
        searchForTrip(earliestTimeNotAlightingAt(TIME_A1), STOP_2)
                .assertNoTripFound();
    }

    @Test
    public void alightFirstTrip() {
        searchForTrip(TIME_LATE, STOP_1)
                .assertTripFound()
                .withIndex(TRIP_B_INDEX)
                .withAlightTime(TIME_B0);

        searchForTrip(TIME_LATE, STOP_2)
                .assertTripFound()
                .withIndex(TRIP_B_INDEX)
                .withAlightTime(TIME_B1);
    }

    @Test
    public void findLastTripWithTheMinimumPossibleSlack() {
        searchForTrip(earliestTimeAlightingAt(TIME_B0), STOP_1)
                .assertTripFound()
                .withIndex(TRIP_B_INDEX)
                .withAlightTime(TIME_B0);

        searchForTrip(earliestTimeAlightingAt(TIME_B1), STOP_2)
                .assertTripFound()
                .withIndex(TRIP_B_INDEX)
                .withAlightTime(TIME_B1);
    }

    @Test
    public void findLastAvailableTripButNotSkippedTrips() {
        // At stop 1
        // Search for the last trip before trip B; expect Trip A
        searchForTrip(earliestTimeNotAlightingAt(TIME_B0), STOP_1)
                .assertTripFound()
                .withIndex(TRIP_A_INDEX)
                .withAlightTime(TIME_A0);

        // At stop 2
        // Search for the last trip before trip B; expect Trip A
        searchForTrip(earliestTimeNotAlightingAt(TIME_B1), STOP_2)
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
        int TRIP_INDEX_A = 0;
        int TRIP_INDEX_B = 1;
        withTrips(tripA, tripB);

        // Then we expect to find trip B when 'tripIndexLowerBound' is A´s index
        searchForTrip(TIME_LATE, STOP_1, TRIP_INDEX_A)
                .assertTripFound()
                .withAlightTime(TIME_B0)
                .withIndex(TRIP_INDEX_B);

        // An then no trip if 'tripIndexLowerBound' equals the last trip index (B´s index)
        searchForTrip(TIME_LATE, STOP_1, TRIP_INDEX_B)
                .assertNoTripFound();
    }

    @Test
    public void findTripWithGivenTripIndexLowerBoundButNotSkippedTrips() {
        // Given the default pattern with the following trips: S, A, T, B

        // STOP 1
        // Then we expect to find trip A when `tripIndexLowerBound` is smaller than A´s index
        searchForTrip(earliestTimeNotAlightingAt(TIME_B0), STOP_1, TRIP_A_INDEX - 1)
                .assertTripFound()
                .withAlightTime(TIME_A0)
                .withIndex(TRIP_A_INDEX);

        // But NOT when `tripIndexLowerBound` equals trip A´s index
        searchForTrip(earliestTimeNotAlightingAt(TIME_B0), STOP_1, TRIP_A_INDEX)
                .assertNoTripFound();

        // STOP 2
        // Then we expect to find trip A when `tripIndexLowerBound` is smaller than A´s index
        searchForTrip(earliestTimeNotAlightingAt(TIME_B1), STOP_2, TRIP_A_INDEX - 1)
                .assertTripFound()
                .withAlightTime(TIME_A1)
                .withIndex(TRIP_A_INDEX);

        // But NOT when `tripIndexLowerBound` equals trip A´s index
        searchForTrip(earliestTimeNotAlightingAt(TIME_B1), STOP_2, TRIP_A_INDEX)
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
            tripSchedules.add(createTripScheduleUseingArrivalTimes(arrivalTime));
        }
        useTripPattern(new TestTripPattern(tripSchedules));


        // Search for a trip that alight before the first trip, expect no trip in return
        searchForTrip(earliestTimeNotAlightingAt(firstArrivalTime), STOP_1)
                .assertNoTripFound();

        for (int i = 0; i < N; ++i) {
            int tripAlightTime = dT * (i + 1);
            int okSearchTime = earliestTimeAlightingAt(tripAlightTime);

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
        final int N = TRIPS_BINARY_SEARCH_THRESHOLD;

        // Given a pattern with N + 1 trip schedules
        List<TestRaptorTripSchedule> tripSchedules = new ArrayList<>();

        // Where the first trip is in service
        tripSchedules.add(tripA);
        final int indexA = 0;

        // And where the N next trips are NOT in service, but with acceptable boarding times
        addNTimes(tripSchedules, tripT, N);

        useTripPattern(new TestTripPattern(tripSchedules));

        // Then we expect to find A for both stop 1 and 2
        // Stop 1
        searchForTrip(earliestTimeAlightingAt(TIME_A0), STOP_1)
                .assertTripFound()
                .withIndex(indexA)
                .withAlightTime(TIME_A0);

        // Stop 2
        searchForTrip(earliestTimeAlightingAt(TIME_A1), STOP_2)
                .assertTripFound()
                .withIndex(indexA)
                .withAlightTime(TIME_A1);
    }

    private boolean skip(Object trip) {
        return trip == tripS || trip == tripT;
    }

    private void withTrips(TestRaptorTripSchedule... schedules) {
        useTripPattern(new TestTripPattern(schedules));
    }

    private void withTrips(List<TestRaptorTripSchedule> schedules) {
        useTripPattern(new TestTripPattern(schedules));
    }

    private void useTripPattern(TestTripPattern pattern) {
        this.pattern = pattern;
        this.subject = new TripScheduleAlightSearch<>(
                TRIPS_BINARY_SEARCH_THRESHOLD,
                this.pattern,
                this::skip
        );
    }

    private int earliestTimeAlightingAt(int alightTime) {
        return alightTime;
    }

    private int earliestTimeNotAlightingAt(int alightTime) {
        return alightTime - 1;
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
