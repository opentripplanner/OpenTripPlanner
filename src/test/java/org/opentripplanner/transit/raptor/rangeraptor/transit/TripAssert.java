package org.opentripplanner.transit.raptor.rangeraptor.transit;

import org.junit.Assert;
import org.opentripplanner.transit.raptor._shared.TestRaptorTripSchedule;

class TripAssert {
    private final TripScheduleSearch<TestRaptorTripSchedule> subject;
    private boolean success;
    private int stopPosition;


    TripAssert(TripScheduleSearch<TestRaptorTripSchedule> subject) {
        this.subject = subject;
    }

    /**
     * @param arrivalTime  The search add the boardSlack, so we use 'arrivalTime' as input to the search
     * @param stopPosition The stop position to board
     */
    TripAssert search(int arrivalTime, int stopPosition) {
        this.stopPosition = stopPosition;
        success = subject.search(arrivalTime, stopPosition);
        return this;
    }

    /**
     * @param arrivalTime    The search add the boardSlack, so we use 'arrivalTime' as input to the search
     * @param stopPosition   The stop position to board
     * @param tripIndexLimit the index of a previous tripSchedule found, used to search for a better trip. (exclusive)
     */
    TripAssert search(int arrivalTime, int stopPosition, int tripIndexLimit) {
        this.stopPosition = stopPosition;
        success = subject.search(arrivalTime, stopPosition, tripIndexLimit);
        return this;
    }

    void assertNoTripFound() {
        Assert.assertFalse("No trip expected, but trip found with index: " + subject.getCandidateTripIndex(), success);
    }

    TripAssert assertTripFound() {
        Assert.assertTrue("Trip expected, but trip found", success);
        return this;
    }

    TripAssert withIndex(int expectedTripIndex) {
        Assert.assertEquals("Trip index", expectedTripIndex, subject.getCandidateTripIndex());
        return this;
    }

    TripAssert withBoardTime(int expectedBoardTime) {
        Assert.assertEquals("Board time", expectedBoardTime, subject.getCandidateTrip().departure(stopPosition));
        return this;
    }

    TripAssert withAlightTime(int expectedBoardTime) {
        Assert.assertEquals("Board time", expectedBoardTime, subject.getCandidateTrip().arrival(stopPosition));
        return this;
    }
}
