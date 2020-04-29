package org.opentripplanner.transit.raptor.rangeraptor.transit;

import org.junit.Test;
import org.opentripplanner.transit.raptor._shared.TestRaptorTripSchedule;
import org.opentripplanner.transit.raptor._shared.TestRoute;
import org.opentripplanner.transit.raptor.api.transit.RaptorRoute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TripScheduleBoardSearchTest {

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

    /* Stop position in pattern */
    private static final int STOP_1 = 0;
    private static final int STOP_2 = 1;

    private static final int TRIP_A_INDEX = 0;
    private static final int TRIP_B_INDEX = 1;
    private static final int TRIP_C_INDEX = 2;

    // Trips in service
    private TestRaptorTripSchedule tripA = TestRaptorTripSchedule
            .create("T-A")
            .withBoardTimes(TIME_A1, TIME_A2)
            .build();
    private TestRaptorTripSchedule tripB = TestRaptorTripSchedule
            .create("T-B")
            .withBoardTimes(TIME_B1, TIME_B2)
            .build();
    private TestRaptorTripSchedule tripC = TestRaptorTripSchedule
            .create("T-C")
            .withBoardTimes(TIME_C1, TIME_C2)
            .build();

    // Trip pattern with trip A, B, C.
    private RaptorRoute<TestRaptorTripSchedule> route = new TestRoute(tripA, tripB, tripC);

    // The service under test - the subject
    private TripScheduleBoardSearch<TestRaptorTripSchedule> subject = new TripScheduleBoardSearch<>(
            TRIPS_BINARY_SEARCH_THRESHOLD, route.timetable()
    );

    @Test
    public void noTripFoundAfterLastTripInServiceDeparture() {
        // When:
        //   Searching for a trip that board after the last trip i service (Trip C)
        // Then:
        //   No trips are expected as a result

        // Stop 1:
        searchForTrip(TIME_C1 + 1, STOP_1).assertNoTripFound();

        // Stop 2:
        searchForTrip(TIME_C2 + 1, STOP_2).assertNoTripFound();
    }

    @Test
    public void boardFirstTrip() {
        searchForTrip(TIME_0, STOP_1)
                .assertTripFound()
                .withIndex(TRIP_A_INDEX)
                .withBoardTime(TIME_A1);

        searchForTrip(TIME_0, STOP_2)
                .assertTripFound()
                .withIndex(TRIP_A_INDEX)
                .withBoardTime(TIME_A2);
    }

    @Test
    public void boardFirstTripWithTheMinimumPossibleSlack() {
        searchForTrip(TIME_A1, STOP_1)
                .assertTripFound()
                .withIndex(TRIP_A_INDEX)
                .withBoardTime(TIME_A1);

        // Assert board next trip for: time + 1 second
        searchForTrip(TIME_A1+1, STOP_1).assertTripFound().withIndex(TRIP_B_INDEX);

        searchForTrip(TIME_A2, STOP_2)
                .assertTripFound()
                .withIndex(TRIP_A_INDEX)
                .withBoardTime(TIME_A2);

        // Assert board next trip for: time + 1 second
        searchForTrip(TIME_A2+1, STOP_2).assertTripFound().withIndex(TRIP_B_INDEX);
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
                .withBoardTime(TIME_A1)
                .withIndex(TRIP_INDEX_A);

        // An then no trip if `tripIndexUpperBound` equals the first trip index (A´s index)
        searchForTrip(TIME_0, STOP_1, TRIP_INDEX_A)
                .assertNoTripFound();
    }

    @Test
    public void boardFirstAvailableTripForABigNumberOfTrips() {
        // For a pattern with N trip schedules,
        // where the first trip departure is at time 1000 and incremented by 1000.
        // We use 1 stop (we search for boardings, we do not care if we can alight)
        final int N = 7 * TRIPS_BINARY_SEARCH_THRESHOLD + 3;
        final int dT = 1000;

        List<TestRaptorTripSchedule> tripSchedules = new ArrayList<>();
        int departureTime = 1000;
        int latestDepartureTime = -1;

        for (int i = 0; i < N; ++i, departureTime += dT) {
            tripSchedules.add(TestRaptorTripSchedule
                    .create("T-N")
                    .withBoardTimes(departureTime)
                    .build());
            latestDepartureTime = departureTime;
        }
        useTripPattern(new TestRoute(tripSchedules));


        // Search for a trip that board after the last trip, expect no trip in return
        searchForTrip(latestDepartureTime + 1, STOP_1)
                .assertNoTripFound();

        for (int i = 0; i < N; ++i) {
            int tripBoardTime = dT * (i + 1);
            int okArrivalTime = tripBoardTime;

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

    private void withTrips(TestRaptorTripSchedule... schedules) {
        useTripPattern(new TestRoute(schedules));
    }

    private void withTrips(List<TestRaptorTripSchedule> schedules) {
        useTripPattern(new TestRoute(schedules));
    }

    private void useTripPattern(TestRoute route) {
        this.route = route;
        this.subject = new TripScheduleBoardSearch<>(
                TRIPS_BINARY_SEARCH_THRESHOLD,
                this.route.timetable()
        );
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
