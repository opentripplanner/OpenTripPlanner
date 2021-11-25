package org.opentripplanner.transit.raptor.rangeraptor.transit;

import org.junit.Assert;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripScheduleBoardOrAlightEvent;

class TripAssert {
    private final TripScheduleSearch<TestTripSchedule> subject;
    private RaptorTripScheduleBoardOrAlightEvent<TestTripSchedule> result;
    private int stopPosition;


    TripAssert(TripScheduleSearch<TestTripSchedule> subject) {
        this.subject = subject;
    }

    /**
     * @param arrivalTime  The search add the boardSlack, so we use 'arrivalTime' as input to the search
     * @param stopPosition The stop position to board
     */
    TripAssert search(int arrivalTime, int stopPosition) {
        this.stopPosition = stopPosition;
        result = subject.search(arrivalTime, stopPosition);
        return this;
    }

    /**
     * @param arrivalTime    The search add the boardSlack, so we use 'arrivalTime' as input to the search
     * @param stopPosition   The stop position to board
     * @param tripIndexLimit the index of a previous tripSchedule found, used to search for a better trip. (exclusive)
     */
    TripAssert search(int arrivalTime, int stopPosition, int tripIndexLimit) {
        this.stopPosition = stopPosition;
        result = subject.search(arrivalTime, stopPosition, tripIndexLimit);
        return this;
    }

    void assertNoTripFound() {
        Assert.assertNull(
                "No trip expected, but trip found with index: " + result,
                result
        );
    }

    TripAssert assertTripFound() {
        Assert.assertNotNull("Trip expected, but trip found", result);
        return this;
    }

    TripAssert withIndex(int expectedTripIndex) {
        Assert.assertEquals("Trip index", expectedTripIndex, result.getTripIndex());
        return this;
    }

    TripAssert withBoardTime(int expectedBoardTime) {
        Assert.assertEquals("Board time", expectedBoardTime, result.getTrip().departure(stopPosition));
        return this;
    }

    TripAssert withAlightTime(int expectedBoardTime) {
        Assert.assertEquals("Board time", expectedBoardTime, result.getTrip().arrival(stopPosition));
        return this;
    }
}
