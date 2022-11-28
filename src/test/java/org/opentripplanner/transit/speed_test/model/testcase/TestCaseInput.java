package org.opentripplanner.transit.speed_test.model.testcase;

import java.util.Collection;

public record TestCaseInput(TestCaseDefinition definition, Collection<Result> expectedResults) {
  public TestCase createTestCase(boolean skipCost) {
    return new TestCase(
      definition,
      new TestCaseResults(definition().id(), skipCost, expectedResults)
    );
  }
}
