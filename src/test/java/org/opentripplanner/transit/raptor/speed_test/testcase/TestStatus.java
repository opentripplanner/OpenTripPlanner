package org.opentripplanner.transit.raptor.speed_test.testcase;


/**
 * A test have a status depending on the test matching after the test is run. The
 * NA status is used to signal that the test is not run or that the status is unknown.
 */
enum TestStatus {
    NA("Status not set"),
    OK("OK"),
    WARN("WARN! Not expected"),
    FAILED("FAILED! Expected");
    final String label;

    TestStatus(String label) {
        this.label = label;
    }

    private boolean is(TestStatus value) {
        return this == value;
    }

    public boolean failed() {
        return is(FAILED);
    }

    public boolean ok() {
        return is(OK);
    }
}
