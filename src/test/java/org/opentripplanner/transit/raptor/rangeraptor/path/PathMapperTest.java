package org.opentripplanner.transit.raptor.rangeraptor.path;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.ACCESS_COST;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.ACCESS_START;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.BASIC_PATH_AS_STRING;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.EGRESS_COST;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.EGRESS_END;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.LINE_11_COST;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.LINE_21_COST;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.LINE_31_COST;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.SLACK_PROVIDER;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.TOTAL_COST;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.TRIP_DURATION;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.TX_COST;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.basicTripByForwardSearch;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.basicTripByReverseSearch;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.lifeCycle;
import static org.opentripplanner.transit.raptor._data.stoparrival.FlexAccessAndEgressPathTestCase.FLEX_PATH_A;
import static org.opentripplanner.transit.raptor._data.stoparrival.FlexAccessAndEgressPathTestCase.FLEX_PATH_B;
import static org.opentripplanner.transit.raptor._data.stoparrival.FlexAccessAndEgressPathTestCase.FLEX_PATH_W_OPENING_HOURS_A;
import static org.opentripplanner.transit.raptor._data.stoparrival.FlexAccessAndEgressPathTestCase.FLEX_PATH_W_OPENING_HOURS_B;
import static org.opentripplanner.transit.raptor._data.stoparrival.FlexAccessAndEgressPathTestCase.flex_case_A_forwardSearch;
import static org.opentripplanner.transit.raptor._data.stoparrival.FlexAccessAndEgressPathTestCase.flex_case_B_forwardSearch;
import static org.opentripplanner.transit.raptor._data.stoparrival.FlexAccessAndEgressPathTestCase.flex_case_A_w_openingHours_forwardSearch;
import static org.opentripplanner.transit.raptor._data.stoparrival.FlexAccessAndEgressPathTestCase.flex_case_B_w_openingHours_forwardSearch;
import static org.opentripplanner.transit.raptor.api.transit.RaptorCostConverter.toOtpDomainCost;

import org.junit.Test;
import org.opentripplanner.transit.raptor._data.RaptorTestConstants;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.path.PathLeg;
import org.opentripplanner.util.time.TimeUtils;

public class PathMapperTest implements RaptorTestConstants {

    /* BASIC CASES */

    @Test
    public void mapToPathBasicForwardSearch() {
        // Given:
        var destArrival = basicTripByForwardSearch();
        var mapper = new ForwardPathMapper<TestTripSchedule>(SLACK_PROVIDER, lifeCycle());

        //When:
        Path<TestTripSchedule> path = mapper.mapToPath(destArrival);

        // Then: verify path - should be the same for reverse and forward mapper
        assertPath(path);
    }

    @Test
    public void mapToPathBasicReverseSearch() {
        // Given:
        var destArrival = basicTripByReverseSearch();
        var mapper = new ReversePathMapper<TestTripSchedule>(SLACK_PROVIDER, lifeCycle());

        //When:
        Path<TestTripSchedule> path = mapper.mapToPath(destArrival);

        // Then: verify path - should be the same for reverse and forward mapper
        assertPath(path);
    }


    /* FLEX CASES - FORWARD SEARCH */

    @Test
    public void mapToPathForFlexCaseAForwardSearch() {
        runtTestFlexForward(flex_case_A_forwardSearch(), FLEX_PATH_A);
    }

    @Test
    public void mapToPathForFlexWOpeningHoursCaseAForwardSearch() {
        runtTestFlexForward(flex_case_A_w_openingHours_forwardSearch(), FLEX_PATH_W_OPENING_HOURS_A);
    }

    @Test
    public void mapToPathForFlexCaseBForwardSearch() {
        runtTestFlexForward(flex_case_B_forwardSearch(), FLEX_PATH_B);
    }

    @Test
    public void mapToPathForFlexWOpeningHoursCaseBForwardSearch() {
        runtTestFlexForward(flex_case_B_w_openingHours_forwardSearch(), FLEX_PATH_W_OPENING_HOURS_B);
    }

    /* FLEX CASES - REVERSE SEARCH */

    /* TODO */

    private void runtTestFlexForward(
            DestinationArrival<TestTripSchedule> destArrival,
            String expected
    ) {
        // Given:
        var mapper = new ForwardPathMapper<TestTripSchedule>(SLACK_PROVIDER, lifeCycle());
        // When:
        Path<TestTripSchedule> path = mapper.mapToPath(destArrival);
        // Then:
        assertEquals(path.toStringDetailed(), expected, path.toString());
    }

    private void runtTestFlexReverse(
            DestinationArrival<TestTripSchedule> destArrival,
            String expected
    ) {
        // Given:
        var mapper = new ReversePathMapper<TestTripSchedule>(SLACK_PROVIDER, lifeCycle());
        // When:
        Path<TestTripSchedule> path = mapper.mapToPath(destArrival);
        // Then:
        assertEquals(expected, path.toString());
    }

    private void assertPath(Path<TestTripSchedule> path) {
        PathLeg<?> leg = path.accessLeg();
        assertEquals("Access 10:00-10:03:15(3m15s) ~ 1", leg.toString());
        assertEquals(toOtpDomainCost(ACCESS_COST), leg.generalizedCost());

        leg = leg.nextLeg();
        assertEquals("BUS L11 10:04-10:35(31m) ~ 2", leg.toString());
        assertEquals(toOtpDomainCost(LINE_11_COST), leg.generalizedCost());

        leg = leg.nextLeg();
        assertEquals("Walk 10:35:15-10:39(3m45s) ~ 3", leg.toString());
        assertEquals(toOtpDomainCost(TX_COST), leg.generalizedCost());

        leg = leg.nextLeg();
        assertEquals("BUS L21 11:00-11:23(23m) ~ 4", leg.toString());
        assertEquals(toOtpDomainCost(LINE_21_COST), leg.generalizedCost());

        leg = leg.nextLeg();
        assertEquals("BUS L31 11:40-11:52(12m) ~ 5", leg.toString());
        assertEquals(toOtpDomainCost(LINE_31_COST), leg.generalizedCost());

        leg = leg.nextLeg();
        assertEquals("Egress 11:52:15-12:00(7m45s)", leg.toString());
        assertEquals(toOtpDomainCost(EGRESS_COST), leg.generalizedCost());

        // Assert some of the most important information
        assertEquals(2, path.numberOfTransfers());
        assertTime("startTime", ACCESS_START, path.startTime());
        assertTime("endTime", EGRESS_END, path.endTime());
        assertTime("duration", TRIP_DURATION, path.travelDurationInSeconds());
        assertEquals(toOtpDomainCost(TOTAL_COST), path.generalizedCost());

        assertEquals(BASIC_PATH_AS_STRING, path.toString());
    }

    private void assertTime(String msg, int expTime, int actualTime) {
        assertEquals(msg, TimeUtils.timeToStrLong(expTime), TimeUtils.timeToStrLong(actualTime));
    }
}