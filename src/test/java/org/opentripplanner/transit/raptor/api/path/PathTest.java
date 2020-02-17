package org.opentripplanner.transit.raptor.api.path;

import org.junit.Assert;
import org.junit.Test;
import org.opentripplanner.transit.raptor._shared.StopArrivalsTestData;
import org.opentripplanner.transit.raptor.api.TestRaptorTripSchedule;
import org.opentripplanner.transit.raptor.util.TimeUtils;

public class PathTest {

    private Path<TestRaptorTripSchedule> subject = StopArrivalsTestData.basicTripAsPath();

    @Test
    public void startTime() {
        Assert.assertEquals("09:53:00", TimeUtils.timeToStrLong(subject.startTime()));
    }

    @Test
    public void endTime() {
        Assert.assertEquals("12:00:00", TimeUtils.timeToStrLong(subject.endTime()));
    }

    @Test
    public void totalTravelDurationInSeconds() {
        Assert.assertEquals("2:07:00", TimeUtils.timeToStrCompact(subject.totalTravelDurationInSeconds()));
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
                "Walk 5:00 > 1 > Transit 10:00-10:35 > 2 > Walk 3:00 > 3 > Transit 11:00-11:23 > 4 > " +
                        "Transit 11:40-11:53 > 5 > Walk 7:00 (tot: 2:07:00, cost: 60)",
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