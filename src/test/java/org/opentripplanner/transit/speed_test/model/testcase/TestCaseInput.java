package org.opentripplanner.transit.speed_test.model.testcase;

import java.util.Objects;
import javax.annotation.Nullable;

public record TestCaseInput(
  TestCaseDefinition definition,
  @Nullable ResultsByProfile expectedResults
) {
  public TestCaseInput {
    Objects.requireNonNull(definition);
    expectedResults = expectedResults == null ? new ResultsByProfile() : expectedResults;
  }

  public TestCase createTestCase(boolean skipCost) {
    return new TestCase(
      definition,
      new TestCaseResults(definition().id(), skipCost, expectedResults)
    );
  }
}
