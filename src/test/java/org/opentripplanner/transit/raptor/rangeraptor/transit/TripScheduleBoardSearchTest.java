package org.opentripplanner.transit.raptor.rangeraptor.transit;

import org.junit.Test;
import org.opentripplanner.transit.raptor.api.TestTripPattern;
import org.opentripplanner.transit.raptor.api.TestTripSchedule;
import org.opentripplanner.transit.raptor.api.transit.TripPatternInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.opentripplanner.transit.raptor.api.TestTripSchedule.createTripScheduleUseingDepartureTimes;

public class TripScheduleBoardSearchTest {

    /*
     * To test board search we need a trip pattern, we will create
     * a trip pattern with 4 trips and 2 stops. This will cover most
     * of the simple cases:
     *
     * Trip:  |  A   |  S   |  B   |  T
     * Stop 1 | 1000 | 1900 | 2000 | 2100
     * Stop 2 | 1500 | 2600 | 2500 | 2400
     *
     * Note:
     * - All times are board times, we do not care about the alight times in this test.
     * - Trip S and T is not in service
     * - Trip S depart from stop 1 before trip B, but depart from stop 2 after trip B.
     * - Trip T depart from stop 1 after trip B, but depart from stop 2 before trip B.
     *
     * The TripScheduleBoardSearch should handle the above trip variations. The point here
     * is that trip B, S and T is in order at stop 1, but not at stop 2.
     */


    /**
     * We use a relatively small prime number as the binary search threshold. This make it simpler to
     * construct a good test.
     */
    private static final int TRIPS_BINARY_SEARCH_THRESHOLD = 7;

    /** A time before all other times */
    private static final int TIME_0 = 0;

    private static final int TIME_A0 = 1000;
    private static final int TIME_A1 = 1500;

    private static final int TIME_B0 = 2000;
    private static final int TIME_B1 = 2500;

    private static final int TIME_S0 = 1900;
    private static final int TIME_S1 = 2600;

    private static final int TIME_T0 = 2100;
    private static final int TIME_T1 = 2400;

    /* Stop position in pattern */
    private static final int STOP_1 = 0;
    private static final int STOP_2 = 1;

    private static final int TRIP_A_INDEX = 0;
    private static final int TRIP_B_INDEX = 2;

    // Trips in service
    private TestTripSchedule tripA = createTripScheduleUseingDepartureTimes(TIME_A0, TIME_A1);
    private TestTripSchedule tripB = createTripScheduleUseingDepartureTimes(TIME_B0, TIME_B1);

    // Trips not in service
    private TestTripSchedule tripS = createTripScheduleUseingDepartureTimes(TIME_S0, TIME_S1);
    private TestTripSchedule tripT = createTripScheduleUseingDepartureTimes(TIME_T0, TIME_T1);

    // Trip pattern with trip A, S, B, T.
    private TripPatternInfo<TestTripSchedule> pattern = new TestTripPattern(tripA, tripS, tripB, tripT);

    // The service under test - the subject
    private TripScheduleBoardSearch<TestTripSchedule> subject = new TripScheduleBoardSearch<>(
            TRIPS_BINARY_SEARCH_THRESHOLD, pattern, this::skip
    );

    @Test
    public void noTripFoundAfterLastTripInServiceDeparture() {
        // When:
        //   Searching for a trip that board after the last trip i service (Trip B)
        // Then:
        //   No trips are expected as a result
        // Stop 1: (Trip T depart after B, but is not in service)
        searchForTrip(latestTimeNotBoardingAt(TIME_B0), STOP_1)
                .assertNoTripFound();
        // Stop 1: (Trip S depart after B, but is not in service)
        searchForTrip(latestTimeNotBoardingAt(TIME_B1), STOP_2)
                .assertNoTripFound();
    }

    @Test
    public void boardFirstTrip() {
        searchForTrip(TIME_0, STOP_1)
                .assertTripFound()
                .withIndex(TRIP_A_INDEX)
                .withBoardTime(TIME_A0);

        searchForTrip(TIME_0, STOP_2)
                .assertTripFound()
                .withIndex(TRIP_A_INDEX)
                .withBoardTime(TIME_A1);
    }

    @Test
    public void boardFirstTripWithTheMinimumPossibleSlack() {
        searchForTrip(latestTimeToBoardAt(TIME_A0), STOP_1)
                .assertTripFound()
                .withIndex(TRIP_A_INDEX)
                .withBoardTime(TIME_A0);

        searchForTrip(latestTimeToBoardAt(TIME_A1), STOP_2)
                .assertTripFound()
                .withIndex(TRIP_A_INDEX)
                .withBoardTime(TIME_A1);
    }

    @Test
    public void boardFirstAvailableTripButNotSkippedTrips() {
        // At stop 1
        // Search for the next trip after trip A; expect Trip B
        searchForTrip(latestTimeNotBoardingAt(TIME_A0), STOP_1)
                .assertTripFound()
                .withIndex(TRIP_B_INDEX)
                .withBoardTime(TIME_B0);

        // At stop 2
        // Search for the next trip after trip A; expect Trip B
        searchForTrip(latestTimeNotBoardingAt(TIME_A1), STOP_2)
                .assertTripFound()
                .withIndex(TRIP_B_INDEX)
                .withBoardTime(TIME_B1);
    }

    @Test
    public void noTripsToBoardInEmptyPattern() {
        // The TripScheduleBoardSearch should handle an empty pattern without failing
        // and return no result found (false)
        withTrips(Collections.emptyList());
        searchForTrip(TIME_0, STOP_1)
                .assertNoTripFound();
    }

    @Test
    public void findTripWithGivenTripIndexUpperBound() {
        // Given a pattern with the following trips: A, B
        int TRIP_INDEX_A = 0;
        int TRIP_INDEX_B = 1;
        withTrips(tripA, tripB);

        // Then we expect to find trip A when `tripIndexUpperBound` is B´s index
        searchForTrip(TIME_0, STOP_1, TRIP_INDEX_B)
                .assertTripFound()
                .withBoardTime(TIME_A0)
                .withIndex(TRIP_INDEX_A);

        // An then no trip if `tripIndexUpperBound` equals the first trip index (A´s index)
        searchForTrip(TIME_0, STOP_1, TRIP_INDEX_A)
                .assertNoTripFound();
    }

    @Test
    public void findTripWithGivenTripIndexUpperBoundButNotSkippedTrips() {
        // Given the default pattern with the following trips: A, S, B, T

        // STOP 1
        // Then we expect to find trip B when `tripIndexUpperBound` is larger than B´s index.
        searchForTrip(latestTimeNotBoardingAt(TIME_A0), STOP_1, TRIP_B_INDEX + 1)
                .assertTripFound()
                .withBoardTime(TIME_B0)
                .withIndex(TRIP_B_INDEX);

        // But NOT when `tripIndexUpperBound` equals trip B´s index
        searchForTrip(latestTimeNotBoardingAt(TIME_A0), STOP_1, TRIP_B_INDEX)
                .assertNoTripFound();

        // STOP 2
        // Then we expect to find trip B when `tripIndexUpperBound` is larger than B´s index
        searchForTrip(latestTimeNotBoardingAt(TIME_A1), STOP_2, TRIP_B_INDEX + 1)
                .assertTripFound()
                .withBoardTime(TIME_B1)
                .withIndex(TRIP_B_INDEX);

        // But NOT when `tripIndexUpperBound` equals trip B´s index
        searchForTrip(latestTimeNotBoardingAt(TIME_A1), STOP_2, TRIP_B_INDEX)
                .assertNoTripFound();
    }

    @Test
    public void boardFirstAvailableTripForABigNumberOfTrips() {
        // For a pattern with N trip schedules,
        // where the first trip departure is at time 1000 and incremented by 1000.
        // We use 1 stop (we search for boardings, we do not care if we can alight)
        final int n = TRIPS_BINARY_SEARCH_THRESHOLD;
        final int N = 7 * n + 3;
        final int dT = 1000;

        List<TestTripSchedule> tripSchedules = new ArrayList<>();
        int departureTime = 1000;
        int latestDepartureTime = -1;

        for (int i = 0; i < N; ++i, departureTime += dT) {
            tripSchedules.add(createTripScheduleUseingDepartureTimes(departureTime));
            latestDepartureTime = departureTime;
        }
        useTripPattern(new TestTripPattern(tripSchedules));


        // Search for a trip that board after the last trip, expect no trip in return
        searchForTrip(latestTimeNotBoardingAt(latestDepartureTime), STOP_1)
                .assertNoTripFound();

        for (int i = 0; i < N; ++i) {
            int tripBoardTime = dT * (i + 1);
            int okArrivalTime = latestTimeToBoardAt(tripBoardTime);

            // Search and find trip 'i'
            searchForTrip(okArrivalTime, STOP_1)
                    .assertTripFound()
                    .withIndex(i)
                    .withBoardTime(tripBoardTime);

            // Search and find trip 'i' using the next trip index
            searchForTrip(okArrivalTime, STOP_1, i+1)
                    .assertTripFound()
                    .withIndex(i);

            // Search with a time and index that together exclude trip 'i'
            searchForTrip(tripBoardTime, STOP_1, i)
                    .assertNoTripFound();
        }
    }

    /**
     * If there is a large number of trips not in service, the binary search may return
     * a best guess index witch is lower than the correct trip index. This test make sure
     * such trips are found.
     */
    @Test
    public void assertTripIsFoundEvenIfItIsBeforeTheBinarySearchUpperAndLowerBound() {
        final int N = TRIPS_BINARY_SEARCH_THRESHOLD;

        // Given a pattern with n + 1 trip schedules
        List<TestTripSchedule> tripSchedules = new ArrayList<>();

        // Where the N first trips are NOT in service, but with acceptable boarding times
        addNTimes(tripSchedules, tripS, N);

        // and where the last trip is in service
        tripSchedules.add(tripB);
        final int indexB = tripSchedules.size()-1;

        useTripPattern(new TestTripPattern(tripSchedules));

        // Then we expect to find B for both stop 1 and 2
        // Stop 1
        searchForTrip(latestTimeToBoardAt(TIME_B0), STOP_1)
                .assertTripFound()
                .withIndex(indexB)
                .withBoardTime(TIME_B0);

        // Stop 2
        searchForTrip(latestTimeToBoardAt(TIME_B1), STOP_2)
                .assertTripFound()
                .withIndex(indexB)
                .withBoardTime(TIME_B1);
    }

    private boolean skip(Object trip) {
        return trip == tripS || trip == tripT;
    }

    private void withTrips(TestTripSchedule... schedules) {
        useTripPattern(new TestTripPattern(schedules));
    }

    private void withTrips(List<TestTripSchedule> schedules) {
        useTripPattern(new TestTripPattern(schedules));
    }

    private void useTripPattern(TestTripPattern pattern) {
        this.pattern = pattern;
        this.subject = new TripScheduleBoardSearch<>(
                TRIPS_BINARY_SEARCH_THRESHOLD,
                this.pattern,
                this::skip
        );
    }

    private int latestTimeToBoardAt(int boardTime) {
        return boardTime;
    }

    private int latestTimeNotBoardingAt(int boardTime) {
        return boardTime + 1;
    }


    private static void addNTimes(List<TestTripSchedule> trips, TestTripSchedule tripS, int n) {
        for (int i = 0; i < n; i++) {
            trips.add(tripS);
        }
    }

    private TripAssert searchForTrip(int arrivalTime, int stopPosition) {
        return new TripAssert(subject)
                .search(arrivalTime, stopPosition);
    }

    private TripAssert searchForTrip(int arrivalTime, int stopPosition, int tripIndexUpperBound) {
        return new TripAssert(subject)
                .search(arrivalTime, stopPosition, tripIndexUpperBound);
    }
}
