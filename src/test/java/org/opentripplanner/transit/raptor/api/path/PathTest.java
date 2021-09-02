package org.opentripplanner.transit.raptor.api.path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.ACCESS_START;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.BASIC_PATH_AS_DETAILED_STRING;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.BASIC_PATH_AS_STRING;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.EGRESS_END;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.RAPTOR_ITERATION_START_TIME;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.TOTAL_COST;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.basicTripStops;
import static org.opentripplanner.util.time.TimeUtils.timeToStrCompact;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.util.time.TimeUtils;


public class PathTest {

    private final Path<TestTripSchedule> subject = BasicPathTestCase.basicTripAsPath();

    @Test
    public void rangeRaptorIterationDepartureTime() {
        assertEquals(RAPTOR_ITERATION_START_TIME, subject.rangeRaptorIterationDepartureTime());
    }

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
        assertEquals("2:00", timeToStrCompact(subject.durationInSeconds()));
    }

    @Test
    public void numberOfTransfers() {
        assertEquals(2, subject.numberOfTransfers());
        assertEquals(2, subject.numberOfTransfersExAccessEgress());
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
    public void legStream() {
        assertEquals(6, subject.legStream().count());
    }

    @Test
    public void transitLegs() {
        assertEquals(3, subject.transitLegs().count());
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void nextTransitLeg() {
        TransitPathLeg<?> leg = subject.accessLeg().nextTransitLeg();
        assertEquals("BUS L11 10:04-10:35(31m) ~ 2", leg.toString());

        leg = leg.nextTransitLeg();
        assertEquals("BUS L21 11:00-11:23(23m) ~ 4", leg.toString());

        leg = leg.nextTransitLeg();
        assertEquals("BUS L31 11:40-11:52(12m) ~ 5", leg.toString());

        leg = leg.nextTransitLeg();
        assertNull(leg);
    }

    @Test
    public void listStops() {
        assertEquals(basicTripStops(), subject.listStops());
    }

    @Test
    public void cost() {
        assertEquals(TOTAL_COST, subject.generalizedCost());
    }

    @Test
    public void waitTime() {
        assertEquals(TimeUtils.time("0:39:15"), subject.waitTime());
    }

    @Test
    public void testToString() {
        assertEquals(BASIC_PATH_AS_STRING, subject.toString());
    }

    @Test
    public void testToStringDetailed() {
        assertEquals(BASIC_PATH_AS_DETAILED_STRING, subject.toStringDetailed());
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
        var p0 = Path.dummyPath(0, 10, 20, 10, 10);
        var p1 = Path.dummyPath(0, 11, 20, 10, 10);
        var p2 = Path.dummyPath(0, 10, 19, 10, 10);
        var p3 = Path.dummyPath(0, 10, 20, 9, 10);
        var p4 = Path.dummyPath(0, 10, 20, 10, 9);

        // Order: < EndTime, > StartTime, < Cost, < Transfers
        List<Path<?>> expected = List.of(p2, p1, p4, p3, p0);

        List<Path<?>> paths = List.of(p4, p3, p2, p1, p0).stream().sorted().collect(Collectors.toList());

        assertEquals(expected, paths);
    }
}