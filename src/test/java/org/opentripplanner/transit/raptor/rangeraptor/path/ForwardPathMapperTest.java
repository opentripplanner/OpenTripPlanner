package org.opentripplanner.transit.raptor.rangeraptor.path;

import org.junit.Test;
import org.opentripplanner.transit.raptor._shared.Egress;
import org.opentripplanner.transit.raptor._shared.StopArrivalsTestData;
import org.opentripplanner.transit.raptor.api.TestTripSchedule;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.rangeraptor.transit.TransitCalculator;
import org.opentripplanner.transit.raptor.util.TimeUtils;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.transit.raptor._shared.StopArrivalsTestData.BOARD_SLACK;
import static org.opentripplanner.transit.raptor.rangeraptor.transit.TransitCalculator.testDummyCalculator;

public class ForwardPathMapperTest {
    private static final TransitCalculator CALCULATOR = testDummyCalculator(BOARD_SLACK, true);

    @Test
    public void mapToPathForwardSearch() {
        Egress egress = StopArrivalsTestData.basicTripByForwardSearch();
        DestinationArrival<TestTripSchedule> destArrival = new DestinationArrival<>(
                egress.previous(),
                egress.arrivalTime(),
                egress.additionalCost()
        );

        PathMapper<TestTripSchedule> mapper = CALCULATOR.createPathMapper();

        Path<TestTripSchedule> path = mapper.mapToPath(destArrival);

        Path<TestTripSchedule> expected = StopArrivalsTestData.basicTripAsPath();

        assertEquals(expected.toString(), path.toString());
        assertEquals(expected.numberOfTransfers(), path.numberOfTransfers());
        assertTime("startTime", expected.startTime(), path.startTime());
        assertTime("endTime", expected.endTime(), path.endTime());
        assertTime("totalTravelDurationInSeconds", expected.totalTravelDurationInSeconds(), path.totalTravelDurationInSeconds());
        assertEquals("numberOfTransfers",  expected.numberOfTransfers(), path.numberOfTransfers());
        assertEquals("cost", expected.cost(), path.cost());
        assertEquals(expected, path);
    }

    private void assertTime(String msg, int expTime, int actualTime) {
        assertEquals(msg, TimeUtils.timeToStrLong(expTime), TimeUtils.timeToStrLong(actualTime));
    }
}