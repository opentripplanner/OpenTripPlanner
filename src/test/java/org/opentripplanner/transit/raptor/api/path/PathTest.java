package org.opentripplanner.transit.raptor.api.path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.ACCESS_START;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.BASIC_PATH_AS_STRING;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.EGRESS_END;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.TOTAL_COST;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.basicTripStops;
import static org.opentripplanner.transit.raptor.api.transit.RaptorCostConverter.toOtpDomainCost;
import static org.opentripplanner.util.time.TimeUtils.timeToStrCompact;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;


public class PathTest {

    private final Path<TestTripSchedule> subject = BasicPathTestCase.basicTripAsPath();

    @Test
    public void startTime() {
        assertEquals(ACCESS_START, subject.startTime());
    }

    @Test
    public void endTime() {
        assertEquals(EGRESS_END, subject.endTime());
    }

    @Test
    public void totalTravelDurationInSeconds() {
        assertEquals("2:00", timeToStrCompact(subject.travelDurationInSeconds()));
    }

    @Test
    public void numberOfTransfers() {
        assertEquals(2, subject.numberOfTransfers());
    }

    @Test
    public void accessLeg() {
        assertNotNull(subject.accessLeg());
    }

    @Test
    public void egressLeg() {
        assertNotNull(subject.egressLeg());
    }

    @Test
    public void listStops() {
        assertEquals(basicTripStops(), subject.listStops());
    }

    @Test
    public void cost() {
        assertEquals(toOtpDomainCost(TOTAL_COST), subject.generalizedCost());
    }

    @Test
    public void testToString() {
        assertEquals(BASIC_PATH_AS_STRING, subject.toString());
    }

    @Test
    public void testToStringDetailed() {
        assertEquals(
                "Walk 3m15s 10:00 10:03:15 $390 ~ 1 45s ~ "
                        + "BUS L11 10:04 10:35 31m $1998 ~ 2 15s ~ "
                        + "Walk 3m45s 10:35:15 10:39 $450 ~ 3 21m ~ "
                        + "BUS L21 11:00 11:23 23m $2640 ~ 4 17m ~ "
                        + "BUS L31 11:40 11:52 12m $1776 ~ 5 15s ~ "
                        + "Walk 7m45s 11:52:15 12:00 $930 "
                        + "[10:00 12:00 2h $8184]",
                subject.toStringDetailed()
        );
    }
    @Test
    public void equals() {
        assertEquals(BasicPathTestCase.basicTripAsPath(), subject);
    }

    @Test
    public void testHashCode() {
        assertEquals(BasicPathTestCase.basicTripAsPath().hashCode(), subject.hashCode());
    }

    @Test
    public void testCompareTo() {
        var p0 = Path.dummyPath(0, 1, 10, 10, 10);
        var p1 = Path.dummyPath(0, 6, 12, 9, 9);
        var p2 = Path.dummyPath(0, 5, 12, 8, 7);
        var p3 = Path.dummyPath(0, 5, 12, 7, 8);

        List<Path<?>> expected = List.of(p0, p1, p2, p3);

        List<Path<?>> paths = List.of(p3, p2, p1, p0).stream().sorted().collect(Collectors.toList());

        assertEquals(expected, paths);
    }
}