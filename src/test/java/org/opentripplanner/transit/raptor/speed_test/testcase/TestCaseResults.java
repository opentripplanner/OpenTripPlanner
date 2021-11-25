package org.opentripplanner.transit.raptor.speed_test.testcase;

import org.opentripplanner.routing.util.DiffTool;
import org.opentripplanner.transit.raptor.speed_test.model.Itinerary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * Contains the expected, actual and matched test results. The responsibility is
 * match all expected with actual results and produce a list of results:
 * <ul>
 *     <li> Matched actual results, status : OK
 *     <li> Expected results NOT found in actual results, status: FAILED
 *     <li> Actual results NOT found in expected, status: WARN
 * </ul>
 */
class TestCaseResults {
    private final String testCaseId;
    private final List<DiffTool.Entry<Result>> matchedResults = new ArrayList<>();
    private final List<Result> expected = new ArrayList<>();
    private final List<Result> actual = new ArrayList<>();
    private TestStatus status = TestStatus.NA;
    private final boolean skipCost;

    TestCaseResults(String testCaseId, boolean skipCost) {
        this.testCaseId = testCaseId;
        this.skipCost  = skipCost;
    }

    void addExpectedResult(Result expectedResult) {
        this.expected.add(expectedResult);
    }

    void matchItineraries(Collection<Itinerary> itineraries) {
        actual.addAll(ItineraryResultMapper.map(testCaseId, itineraries, skipCost));
        matchedResults.clear();
        matchedResults.addAll(DiffTool.diff(expected, actual, Result.comparator(skipCost)));
        status = resolveStatus();
    }

    List<Result> actualResults() {
        return actual;
    }

    /**
     * All test results are OK.
     */
    public boolean success() {
        return status.ok();
    }

    /**
     * At least one expected result is missing.
     */
    public boolean failed() {
        return status.failed();
    }

    /**
     * No test results is found. This indicate that the test is not run or
     * that the route had no itineraries.
     */
    public boolean noResults() {
        return matchedResults.isEmpty();
    }

    private TestStatus resolveStatus() {
        if(matchedResults.isEmpty()) { return TestStatus.NA; }
        if(matchedResults.stream().anyMatch(DiffTool.Entry::leftOnly)) { return TestStatus.FAILED; }
        if(matchedResults.stream().anyMatch(DiffTool.Entry::rightOnly)) { return TestStatus.WARN; }
        return TestStatus.OK;
    }

    @Override
    public String toString() {
        return TableTestReport.report(matchedResults);
    }

}
