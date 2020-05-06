package org.opentripplanner.transit.raptor.api.path;

import org.junit.Assert;
import org.junit.Test;
import org.opentripplanner.transit.raptor._shared.StopArrivalsTestData;
import org.opentripplanner.transit.raptor._shared.TestRaptorTripSchedule;
import org.opentripplanner.transit.raptor.util.TimeUtils;

import static org.opentripplanner.transit.raptor._shared.StopArrivalsTestData.A_START;
import static org.opentripplanner.transit.raptor._shared.StopArrivalsTestData.E_END;

public class PathTest {

    private final Path<TestRaptorTripSchedule> subject = StopArrivalsTestData.basicTripAsPath();

    public PathTest() {
        System.out.println(subject);
    }

    @Test
    public void startTime() {
        Assert.assertEquals(A_START, subject.startTime());
    }

    @Test
    public void endTime() {
        Assert.assertEquals(E_END, subject.endTime());
    }

    @Test
    public void totalTravelDurationInSeconds() {
        Assert.assertEquals("2:00", TimeUtils.timeToStrCompact(subject.travelDurationInSeconds()));
    }

    @Test
    public void numberOfTransfers() {
        Assert.assertEquals(2, subject.numberOfTransfers());
    }

    @Test
    public void accessLeg() {
        Assert.assertNotNull(subject.accessLeg());
    }

    @Test
    public void egressLeg() {
        Assert.assertNotNull(subject.egressLeg());
    }

    @Test
    public void listStops() {
        Assert.assertEquals(StopArrivalsTestData.basicTripStops(), subject.listStops());
    }

    @Test
    public void cost() {
        Assert.assertEquals(60, subject.cost());
    }

    @Test
    public void testToString() {
        Assert.assertEquals(
                "Walk 3m ~ 1 ~ BUS 10:05 10:35 ~ 2 ~ Walk 3m ~ 3 ~ "
                        + "BUS 11:00 11:23 ~ 4 ~ BUS 11:40 11:52 ~ 5 ~ Walk 7m "
                        + "[10:00:00 12:00:00 2h, cost: 60]",
                subject.toString()
        );
    }

    @Test
    public void equals() {
        Assert.assertEquals(StopArrivalsTestData.basicTripAsPath(), subject);
    }

    @Test
    public void testHashCode() {
        Assert.assertEquals(StopArrivalsTestData.basicTripAsPath().hashCode(), subject.hashCode());
    }
}