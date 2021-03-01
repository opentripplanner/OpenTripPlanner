package org.opentripplanner.transit.raptor.api.path;

import org.junit.Assert;
import org.junit.Test;
import org.opentripplanner.transit.raptor._data.stoparrival.StopArrivalsTestData;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.util.time.TimeUtils;

import java.util.List;
import java.util.stream.Collectors;

import static org.opentripplanner.transit.raptor._data.stoparrival.StopArrivalsTestData.A_START;
import static org.opentripplanner.transit.raptor._data.stoparrival.StopArrivalsTestData.E_END;


public class PathTest {

    private final Path<TestTripSchedule> subject = StopArrivalsTestData.basicTripAsPath();

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
                "Walk 3m ~ 1 ~ BUS T1 10:05 10:35 ~ 2 ~ Walk 3m ~ 3 ~ "
                        + "BUS T2 11:00 11:23 ~ 4 ~ BUS T3 11:40 11:52 ~ 5 ~ Walk 7m "
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

    @Test
    public void testCompareTo() {
        var p0 = Path.dummyPath(0, 1, 10, 10, 10);
        var p1 = Path.dummyPath(0, 6, 12, 9, 9);
        var p2 = Path.dummyPath(0, 5, 12, 8, 7);
        var p3 = Path.dummyPath(0, 5, 12, 7, 8);

        List<Path<?>> expected = List.of(p0, p1, p2, p3);

        List<Path<?>> paths = List.of(p3, p2, p1, p0).stream().sorted().collect(Collectors.toList());

        Assert.assertEquals(expected, paths);
    }
}