package org.opentripplanner.transit.raptor.speed_test.model.testcase;

/**
 * The purpose of this exception is to signal that a testcase failed.
 */
public class TestCaseFailedException extends RuntimeException {
    TestCaseFailedException() {
        super("Test assert errors");
    }
}
