package org.opentripplanner.transit.raptor.rangeraptor.path;

import org.junit.Test;
import org.opentripplanner.transit.raptor._data.stoparrival.Egress;
import org.opentripplanner.transit.raptor._data.stoparrival.StopArrivalsTestData;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.path.PathLeg;
import org.opentripplanner.transit.raptor.rangeraptor.WorkerLifeCycle;
import org.opentripplanner.util.time.TimeUtils;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.transit.raptor._data.stoparrival.StopArrivalsTestData.basicTripByReverseSearch;
import static org.opentripplanner.transit.raptor._data.stoparrival.StopArrivalsTestData.lifeCycle;
import static org.opentripplanner.transit.raptor._data.transit.TestTransfer.walk;

public class PathMapperTest {

    @Test
    public void mapToPathForwardSearch() {
        // Given:
        Egress egress = StopArrivalsTestData.basicTripByForwardSearch();
        DestinationArrival<TestTripSchedule> destArrival = new DestinationArrival<>(
                walk(egress.previous().stop(), egress.durationInSeconds()),
                egress.previous(),
                egress.arrivalTime(),
                egress.additionalCost()
        );

        WorkerLifeCycle lifeCycle = StopArrivalsTestData.lifeCycle();
        PathMapper<TestTripSchedule> mapper = new ForwardPathMapper<>(
            StopArrivalsTestData.slackProvider(),
            lifeCycle
        );

        //When:
        Path<TestTripSchedule> path = mapper.mapToPath(destArrival);

        // Then: verify path - should be the same for reverse and forward mapper
        assertPath(path);
    }

    @Test
    public void mapToPathReverseSearch() {
        // Given:
        Egress egress = basicTripByReverseSearch();
        DestinationArrival<TestTripSchedule> destArrival = new DestinationArrival<>(walk(egress
            .previous()
            .stop(), egress.durationInSeconds()),
            egress.previous(),
            egress.arrivalTime(),
            egress.additionalCost()
        );
        WorkerLifeCycle lifeCycle = lifeCycle();
        PathMapper<TestTripSchedule> mapper = new ReversePathMapper<>(
            StopArrivalsTestData.slackProvider(),
            lifeCycle
        );

        //When:
        Path<TestTripSchedule> path = mapper.mapToPath(destArrival);

        // Then: verify path - should be the same for reverse and forward mapper
        assertPath(path);
    }

    private void assertPath(Path<TestTripSchedule> path) {
        PathLeg<?> leg = path.accessLeg();
        assertEquals("Access 10:00-10:03(3m) ~ 1", leg.toString());
        assertEquals(10, leg.generalizedCost());

        leg = leg.nextLeg();
        assertEquals("BUS T1 10:05-10:35(30m) ~ 2", leg.toString());
        assertEquals(10, leg.generalizedCost());

        leg = leg.nextLeg();
        assertEquals("Walk 10:36-10:39(3m) ~ 3", leg.toString());
        assertEquals(10, leg.generalizedCost());

        leg = leg.nextLeg();
        assertEquals("BUS T2 11:00-11:23(23m) ~ 4", leg.toString());
        assertEquals(10, leg.generalizedCost());

        leg = leg.nextLeg();
        assertEquals("BUS T3 11:40-11:52(12m) ~ 5", leg.toString());
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
        assertEquals(msg, TimeUtils.timeToStrLong(expTime), TimeUtils.timeToStrLong(actualTime));
    }
}