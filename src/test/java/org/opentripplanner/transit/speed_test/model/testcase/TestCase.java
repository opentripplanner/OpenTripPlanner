package org.opentripplanner.transit.speed_test.model.testcase;

import java.util.Collection;
import java.util.List;
import org.opentripplanner.model.plan.Itinerary;

/**
 * Hold all information about a test case and its results.
 */
public record TestCase(TestCaseDefinition definition, TestCaseResults results) {
  public static final int NOT_SET = -1;

  public String id() {
    return definition.id();
  }

  /**
   * All test results are OK.
   */
  public boolean success() {
    return results.success();
  }

  public void printResults() {
    if (!results.actualResults().isEmpty()) {
      System.err.println(results);
    }
  }

  /**
   * Verify the result by matching it with the {@code expectedResult} from the csv file.
   */
  public void assertResult(Collection<Itinerary> itineraries, int transitTimeMs, int totalTimeMs) {
    results.matchItineraries(itineraries);
    results.addTimes(transitTimeMs, totalTimeMs);

    if (results.failed()) {
      throw new TestCaseFailedException();
    }
  }

  @Override
  public String toString() {
    return definition.toString();
  }

  public int numberOfResults() {
    return results.actualResults().size();
  }

  public int transitTimeMs() {
    return results.transitTimeMs();
  }

  public int totalTimeMs() {
    return results.totalTimeMs();
  }

  /**
   * The test case is not run or no itineraries found.
   */
  boolean notRunOrNoResults() {
    return results.noResults();
  }

  /**
   * List all results found for this testcase.
   */
  List<Result> actualResults() {
    return results.actualResults();
  }
}
