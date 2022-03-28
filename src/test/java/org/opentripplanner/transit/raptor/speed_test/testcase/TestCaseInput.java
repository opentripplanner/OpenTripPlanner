package org.opentripplanner.transit.raptor.speed_test.testcase;


import java.util.Collection;

public record TestCaseInput(
        TestCaseDefinition definition,
        Collection<Result> expectedResults
) {

    public TestCase createTestCase(boolean skipCost) {
        return new TestCase(definition, new TestCaseResults(definition().id(), skipCost, expectedResults));
    }
}
