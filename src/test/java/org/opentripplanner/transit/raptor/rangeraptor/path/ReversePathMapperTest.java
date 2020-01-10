package org.opentripplanner.transit.raptor.rangeraptor.path;

import org.junit.Test;
import org.opentripplanner.transit.raptor._shared.Egress;
import org.opentripplanner.transit.raptor._shared.StopArrivalsTestData;
import org.opentripplanner.transit.raptor.api.TestTripSchedule;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.rangeraptor.transit.TransitCalculator;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.transit.raptor._shared.StopArrivalsTestData.BOARD_SLACK;
import static org.opentripplanner.transit.raptor._shared.StopArrivalsTestData.basicTripByReverseSearch;
import static org.opentripplanner.transit.raptor.rangeraptor.transit.TransitCalculator.testDummyCalculator;
import static org.opentripplanner.transit.raptor.util.TimeUtils.timeToStrLong;

public class ReversePathMapperTest {
    private static final TransitCalculator CALCULATOR = testDummyCalculator(BOARD_SLACK, false);

    @Test
    public void mapToPathReverseSearch() {
        // Given:
        Egress egress = basicTripByReverseSearch();
        DestinationArrival<TestTripSchedule> destArrival = new DestinationArrival<>(
                egress.previous(),
                egress.arrivalTime(),
                egress.additionalCost()
        );
        Path<TestTripSchedule> expected = StopArrivalsTestData.basicTripAsPath();
        PathMapper<TestTripSchedule> mapper = CALCULATOR.createPathMapper();


        //When:
        Path<TestTripSchedule> path = mapper.mapToPath(destArrival);

        // Then:
        assertTime("startTime", expected.startTime(), path.startTime());
        assertTime("endTime", expected.endTime(), path.endTime());
        assertTime("totalTravelDurationInSeconds", expected.totalTravelDurationInSeconds(), path.totalTravelDurationInSeconds());
        assertEquals("cost", expected.cost(), path.cost());
        assertEquals("numberOfTransfers",  expected.numberOfTransfers(), path.numberOfTransfers());
        assertEquals(expected.toString(), path.toString());
        assertEquals(expected, path);
    }


    private void assertTime(String msg, int expTime, int actualTime) {
        assertEquals(msg, timeToStrLong(expTime), timeToStrLong(actualTime));
    }
}