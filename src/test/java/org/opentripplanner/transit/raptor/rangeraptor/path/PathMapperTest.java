package org.opentripplanner.transit.raptor.rangeraptor.path;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.BASIC_PATH_AS_DETAILED_STRING;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.SLACK_PROVIDER;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.basicTripByForwardSearch;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.basicTripByReverseSearch;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.lifeCycle;
import static org.opentripplanner.transit.raptor._data.stoparrival.FlexAccessAndEgressPathTestCase.flexCaseAForwardSearchAsText;
import static org.opentripplanner.transit.raptor._data.stoparrival.FlexAccessAndEgressPathTestCase.flexCaseAReverseSearchAsText;
import static org.opentripplanner.transit.raptor._data.stoparrival.FlexAccessAndEgressPathTestCase.flexCaseAWithOpeningHoursForwardSearchAsText;
import static org.opentripplanner.transit.raptor._data.stoparrival.FlexAccessAndEgressPathTestCase.flexCaseAWithOpeningHoursReverseSearchAsText;
import static org.opentripplanner.transit.raptor._data.stoparrival.FlexAccessAndEgressPathTestCase.flexCaseBForwardSearchAsText;
import static org.opentripplanner.transit.raptor._data.stoparrival.FlexAccessAndEgressPathTestCase.flexCaseBReverseSearchAsText;
import static org.opentripplanner.transit.raptor._data.stoparrival.FlexAccessAndEgressPathTestCase.flexCaseBWithOpeningHoursForwardSearchAsText;
import static org.opentripplanner.transit.raptor._data.stoparrival.FlexAccessAndEgressPathTestCase.flexCaseBWithOpeningHoursReverseSearchAsText;
import static org.opentripplanner.transit.raptor._data.stoparrival.FlexAccessAndEgressPathTestCase.flexCaseAForwardSearch;
import static org.opentripplanner.transit.raptor._data.stoparrival.FlexAccessAndEgressPathTestCase.flexCaseAReverseSearch;
import static org.opentripplanner.transit.raptor._data.stoparrival.FlexAccessAndEgressPathTestCase.flexCaseAWithOpeningHoursForwardSearch;
import static org.opentripplanner.transit.raptor._data.stoparrival.FlexAccessAndEgressPathTestCase.flexCaseAWithOpeningHoursReverseSearch;
import static org.opentripplanner.transit.raptor._data.stoparrival.FlexAccessAndEgressPathTestCase.flexCaseBForwardSearch;
import static org.opentripplanner.transit.raptor._data.stoparrival.FlexAccessAndEgressPathTestCase.flexCaseBReverseSearch;
import static org.opentripplanner.transit.raptor._data.stoparrival.FlexAccessAndEgressPathTestCase.flexCaseBWithOpeningHoursForwardSearch;
import static org.opentripplanner.transit.raptor._data.stoparrival.FlexAccessAndEgressPathTestCase.flexCaseBWithOpeningHoursReverseSearch;

import org.junit.Test;
import org.opentripplanner.transit.raptor._data.RaptorTestConstants;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.transit.raptor.api.path.Path;

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
        runtTestFlexForward(flexCaseAForwardSearch(), flexCaseAForwardSearchAsText());
    }

    @Test
    public void mapToPathForFlexCaseAWOpeningHoursForwardSearch() {
        runtTestFlexForward(
                flexCaseAWithOpeningHoursForwardSearch(),
                flexCaseAWithOpeningHoursForwardSearchAsText()
        );
    }

    @Test
    public void mapToPathForFlexCaseBForwardSearch() {
        runtTestFlexForward(flexCaseBForwardSearch(), flexCaseBForwardSearchAsText());
    }

    @Test
    public void mapToPathForFlexCaseBWOpeningHoursForwardSearch() {
        runtTestFlexForward(
                flexCaseBWithOpeningHoursForwardSearch(),
                flexCaseBWithOpeningHoursForwardSearchAsText()
        );
    }

    /* FLEX CASES - REVERSE SEARCH */

    @Test
    public void mapToPathForFlexCaseAReverseSearch() {
        runtTestFlexReverse(flexCaseAReverseSearch(), flexCaseAReverseSearchAsText());
    }

    @Test
    public void mapToPathForFlexCaseAWOpeningHoursReverseSearch() {
        runtTestFlexReverse(
                flexCaseAWithOpeningHoursReverseSearch(),
                flexCaseAWithOpeningHoursReverseSearchAsText()
        );
    }

    @Test
    public void mapToPathForFlexCaseBReverseSearch() {
        runtTestFlexReverse(flexCaseBReverseSearch(), flexCaseBReverseSearchAsText());
    }

    @Test
    public void mapToPathForFlexCaseBWOpeningHoursReverseSearch() {
        runtTestFlexReverse(
                flexCaseBWithOpeningHoursReverseSearch(),
                flexCaseBWithOpeningHoursReverseSearchAsText()
        );
    }

    private void runtTestFlexForward(
            DestinationArrival<TestTripSchedule> destArrival,
            String expected
    ) {
        // Given:
        var mapper = new ForwardPathMapper<TestTripSchedule>(SLACK_PROVIDER, lifeCycle());
        // When:
        Path<TestTripSchedule> path = mapper.mapToPath(destArrival);
        // Then:
        assertEquals(path.toStringDetailed(), expected, path.toStringDetailed());
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
        assertEquals(expected, path.toStringDetailed());
    }

    private void assertPath(Path<TestTripSchedule> path) {
        assertEquals(BASIC_PATH_AS_DETAILED_STRING, path.toStringDetailed());
    }
}