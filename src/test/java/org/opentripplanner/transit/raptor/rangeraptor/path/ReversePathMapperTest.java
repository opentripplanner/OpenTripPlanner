package org.opentripplanner.transit.raptor.rangeraptor.path;

import org.junit.Test;
import org.opentripplanner.transit.raptor._shared.Egress;
import org.opentripplanner.transit.raptor._shared.StopArrivalsTestData;
import org.opentripplanner.transit.raptor._shared.TestRaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.path.PathLeg;
import org.opentripplanner.transit.raptor._shared.TestRaptorTransfer;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.transit.raptor._shared.StopArrivalsTestData.basicTripByReverseSearch;
import static org.opentripplanner.transit.raptor.util.TimeUtils.timeToStrLong;

public class ReversePathMapperTest {
    @Test
    public void mapToPathReverseSearch() {
        // Given:
        Egress egress = basicTripByReverseSearch();
        DestinationArrival<TestRaptorTripSchedule> destArrival = new DestinationArrival<>(
                new TestRaptorTransfer(egress.previous().stop(), egress.durationInSeconds()),
                egress.previous(),
                egress.arrivalTime(),
                egress.additionalCost()
        );
        PathMapper<TestRaptorTripSchedule> mapper = new ReversePathMapper<>(
                StopArrivalsTestData.WORKER_LIFE_CYCLE
        );

        //When:
        Path<TestRaptorTripSchedule> path = mapper.mapToPath(destArrival);

        // Then:
        PathLeg<?> leg = path.accessLeg();
        assertEquals("Access 10:00-10:03(3m) -> Stop 1", leg.toString());
        assertEquals(10, leg.generalizedCost());

        leg = leg.nextLeg();
        assertEquals("BUS T1 10:05-10:35(30m) -> Stop 2", leg.toString());
        assertEquals(10, leg.generalizedCost());

        leg = leg.nextLeg();
        assertEquals("Walk 10:36-10:39(3m) -> Stop 3", leg.toString());
        assertEquals(10, leg.generalizedCost());

        leg = leg.nextLeg();
        assertEquals("BUS T2 11:00-11:23(23m) -> Stop 4", leg.toString());
        assertEquals(10, leg.generalizedCost());

        leg = leg.nextLeg();
        assertEquals("BUS T3 11:40-11:52(12m) -> Stop 5", leg.toString());
        assertEquals(10, leg.generalizedCost());

        leg = leg.nextLeg();
        assertEquals("Egress 11:53-12:00(7m)", leg.toString());
        assertEquals(10, leg.generalizedCost());

        // Assert some of the most important information
        assertEquals(2, path.numberOfTransfers());
        assertTime("startTime", StopArrivalsTestData.A_START, path.startTime());
        assertTime("endTime", StopArrivalsTestData.E_END, path.endTime());
        assertTime("duration", StopArrivalsTestData.TRIP_DURATION, path.travelDurationInSeconds());
        assertEquals(60, path.cost());

        assertEquals(
                "Walk 3m ~ 1 ~ BUS T1 10:05 10:35 ~ 2 ~ Walk 3m ~ 3 ~ "
                        + "BUS T2 11:00 11:23 ~ 4 ~ BUS T3 11:40 11:52 ~ 5 ~ Walk 7m "
                        + "[10:00:00 12:00:00 2h, cost: 60]",
                path.toString()
        );
    }

    private void assertTime(String msg, int expTime, int actualTime) {
        assertEquals(msg, timeToStrLong(expTime), timeToStrLong(actualTime));
    }
}