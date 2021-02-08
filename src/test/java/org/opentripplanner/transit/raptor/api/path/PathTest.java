package org.opentripplanner.transit.raptor.api.path;

import org.junit.Test;
import org.opentripplanner.transit.raptor._data.stoparrival.BasicItineraryTestCase;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicItineraryTestCase.ACCESS_START;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicItineraryTestCase.BASIC_PATH_AS_STRING;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicItineraryTestCase.EGRESS_END;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicItineraryTestCase.TOTAL_COST;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicItineraryTestCase.basicTripStops;
import static org.opentripplanner.transit.raptor.api.transit.RaptorCostConverter.toOtpDomainCost;
import static org.opentripplanner.util.time.TimeUtils.timeToStrCompact;


public class PathTest {

    private final Path<TestTripSchedule> subject = BasicItineraryTestCase.basicTripAsPath();

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
    public void equals() {
        assertEquals(BasicItineraryTestCase.basicTripAsPath(), subject);
    }

    @Test
    public void testHashCode() {
        assertEquals(BasicItineraryTestCase.basicTripAsPath().hashCode(), subject.hashCode());
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