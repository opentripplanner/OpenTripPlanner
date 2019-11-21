package org.opentripplanner.transit.raptor.speed_test.testcase;

/**
 * The purpose of this exception is to signal that a testcase failed.
 */
public class TestCaseFailedException extends RuntimeException {
    TestCaseFailedException() {
        super("Test assert errors");
    }

    public TestCaseFailedException(String message) {
        super(message);
    }
}
