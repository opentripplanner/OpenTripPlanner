package org.opentripplanner.transit.speed_test.model.testcase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.util.DiffEntry;
import org.opentripplanner.routing.util.DiffTool;

/**
 * Contains the expected, actual and matched test results. The responsibility is match all expected
 * with actual results and produce a list of results:
 * <ul>
 *     <li> Matched actual results, status : OK
 *     <li> Expected results NOT found in actual results, status: FAILED
 *     <li> Actual results NOT found in expected, status: WARN
 * </ul>
 */
class TestCaseResults {

  private final String testCaseId;
  private final boolean skipCost;
  private final List<Result> expected;
  private final List<Result> actual = new ArrayList<>();
  private final List<DiffEntry<Result>> matchedResults = new ArrayList<>();
  private TestStatus status = TestStatus.NA;
  private int transitTimeMs = 0;
  private int totalTimeMs = 0;

  TestCaseResults(String testCaseId, boolean skipCost, Collection<Result> expected) {
    this.testCaseId = testCaseId;
    this.skipCost = skipCost;
    this.expected = List.copyOf(expected);
  }

  public int transitTimeMs() {
    return transitTimeMs;
  }

  public int totalTimeMs() {
    return totalTimeMs;
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
   * No test results is found. This indicates that the test is not run or that the route had no
   * itineraries.
   */
  public boolean noResults() {
    return matchedResults.isEmpty();
  }

  @Override
  public String toString() {
    return TableTestReport.report(matchedResults);
  }

  void addTimes(int transitTimeMs, int totalTimeMs) {
    this.transitTimeMs = transitTimeMs;
    this.totalTimeMs = totalTimeMs;
  }

  void matchItineraries(Collection<Itinerary> itineraries) {
    actual.addAll(ItineraryResultMapper.map(testCaseId, itineraries));
    matchedResults.clear();
    matchedResults.addAll(DiffTool.diff(expected, actual, Result.comparator(skipCost)));
    status = resolveStatus();
  }

  List<Result> actualResults() {
    return actual;
  }

  private TestStatus resolveStatus() {
    if (matchedResults.isEmpty()) {
      return TestStatus.NA;
    }
    if (matchedResults.stream().anyMatch(DiffEntry::leftOnly)) {
      return TestStatus.FAILED;
    }
    if (matchedResults.stream().anyMatch(DiffEntry::rightOnly)) {
      return TestStatus.WARN;
    }
    return TestStatus.OK;
  }
}
