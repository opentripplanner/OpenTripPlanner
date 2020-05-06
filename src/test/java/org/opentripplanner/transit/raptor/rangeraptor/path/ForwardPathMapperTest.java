package org.opentripplanner.transit.raptor.rangeraptor.path;

import org.junit.Test;
import org.opentripplanner.transit.raptor._shared.Egress;
import org.opentripplanner.transit.raptor._shared.StopArrivalsTestData;
import org.opentripplanner.transit.raptor._shared.TestRaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.path.PathLeg;
import org.opentripplanner.transit.raptor.util.TimeUtils;

import static org.junit.Assert.assertEquals;

public class ForwardPathMapperTest {

    @Test
    public void mapToPathForwardSearch() {
        Egress egress = StopArrivalsTestData.basicTripByForwardSearch();
        DestinationArrival<TestRaptorTripSchedule> destArrival = new DestinationArrival<>(
                null,
                egress.previous(),
                egress.previous().arrivalTime(),
                egress.arrivalTime(),
                egress.additionalCost()
        );

        PathMapper<TestRaptorTripSchedule> mapper = new ForwardPathMapper<>(
            StopArrivalsTestData.SLACK_PROVIDER,
            StopArrivalsTestData.WORKER_LIFE_CYCLE
        );

        Path<TestRaptorTripSchedule> path = mapper.mapToPath(destArrival);

        PathLeg<?> leg = path.accessLeg();
        assertEquals("Access 10:00-10:03(3m) -> Stop 1", leg.toString());

        leg = leg.nextLeg();
        assertEquals("T1 10:05-10:35(30m) -> Stop 2", leg.toString());

        leg = leg.nextLeg();
        assertEquals("Walk 10:36-10:39(3m) -> Stop 3", leg.toString());

        leg = leg.nextLeg();
        assertEquals("T2 11:00-11:23(23m) -> Stop 4", leg.toString());

        leg = leg.nextLeg();
        assertEquals("T3 11:40-11:52(12m) -> Stop 5", leg.toString());

        leg = leg.nextLeg();
        assertEquals("Egress 11:53-12:00(7m)", leg.toString());

        // Assert some of the most important information
        assertEquals(2, path.numberOfTransfers());
        assertTime("startTime", StopArrivalsTestData.A_START, path.startTime());
        assertTime("endTime", StopArrivalsTestData.E_END, path.endTime());
        assertTime("duration", StopArrivalsTestData.TRIP_DURATION, path.travelDurationInSeconds());
        assertEquals(60, path.cost());

        assertEquals(
                "Walk 3m ~ 1 ~ BUS 10:05 10:35 ~ 2 ~ Walk 3m ~ 3 ~ "
                        + "BUS 11:00 11:23 ~ 4 ~ BUS 11:40 11:52 ~ 5 ~ Walk 7m "
                        + "[10:00:00 12:00:00 2h, cost: 60]",
                path.toString()
        );
    }

    private void assertTime(String msg, int expTime, int actualTime) {
        assertEquals(msg, TimeUtils.timeToStrLong(expTime), TimeUtils.timeToStrLong(actualTime));
    }
}