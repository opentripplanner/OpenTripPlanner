package org.opentripplanner.transit.raptor.speed_test.test;

import org.opentripplanner.transit.raptor.speed_test.api.model.Itinerary;

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
    private String testCaseId;
    private List<org.opentripplanner.transit.raptor.speed_test.test.Result> matchedResults = new ArrayList<>();
    private List<org.opentripplanner.transit.raptor.speed_test.test.Result> expected = new ArrayList<>();
    private List<org.opentripplanner.transit.raptor.speed_test.test.Result> actual = new ArrayList<>();
    private org.opentripplanner.transit.raptor.speed_test.test.TestStatus status = org.opentripplanner.transit.raptor.speed_test.test.TestStatus.NA;

    TestCaseResults(String testCaseId) {
        this.testCaseId = testCaseId;
    }

    void addExpectedResult(org.opentripplanner.transit.raptor.speed_test.test.Result expectedResult) {
        this.expected.add(expectedResult);
    }

    void matchItineraries(Collection<Itinerary> itineraries) {
        actual.addAll(ItineraryResultMapper.map(testCaseId, itineraries));
        boolean[] resultsOk = new boolean[actual.size()];

        for (org.opentripplanner.transit.raptor.speed_test.test.Result exp : expected) {
            int i = actual.indexOf(exp);
            if (i == -1) {
                failed(exp);
            } else {
                ok(actual.get(i));
                resultsOk[i] = true;
            }
        }
        // Log all results not matched
        for (int i = 0; i < resultsOk.length; ++i) {
            if (!resultsOk[i]) {
                notExpected(actual.get(i));
            }
        }
    }

    List<org.opentripplanner.transit.raptor.speed_test.test.Result> actualResults() {
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

    @Override
    public String toString() {
        return TableTestReport.report(matchedResults);
    }


    /* private methods */

    private void failed(org.opentripplanner.transit.raptor.speed_test.test.Result result) {
        addResult(org.opentripplanner.transit.raptor.speed_test.test.TestStatus.FAILED, result);
    }

    private void ok(org.opentripplanner.transit.raptor.speed_test.test.Result result) {
        addResult(org.opentripplanner.transit.raptor.speed_test.test.TestStatus.OK, result);
    }

    private void notExpected(org.opentripplanner.transit.raptor.speed_test.test.Result result) {
        addResult(org.opentripplanner.transit.raptor.speed_test.test.TestStatus.WARN, result);
    }

    private void addResult(org.opentripplanner.transit.raptor.speed_test.test.TestStatus status, org.opentripplanner.transit.raptor.speed_test.test.Result result) {
        updateStatus(status);
        result.setStatus(status);
        matchedResults.add(result);
    }

    private void updateStatus(org.opentripplanner.transit.raptor.speed_test.test.TestStatus newStatus) {
        if (status.ordinal() < newStatus.ordinal()) {
            status = newStatus;
        }
    }
}
