package org.opentripplanner.transit.speed_test.model.testcase;

/**
 * A test have a status depending on the test matching after the test is run. The NA status is used
 * to signal that the test is not run or that the status is unknown.
 */
public enum TestStatus {
  NA("Status not set", 10),
  OK("OK", 0),
  WARN("WARN! Not expected", 100),
  FAILED("FAILED! Expected", 200);

  final String label;
  final int severity;

  TestStatus(String label, int severity) {
    this.label = label;
    this.severity = severity;
  }

  public boolean failed() {
    return is(FAILED);
  }

  public boolean notOk() {
    return this != OK;
  }

  public boolean ok() {
    return is(OK);
  }

  public TestStatus highestSeverity(TestStatus other) {
    return severity > other.severity ? this : other;
  }

  private boolean is(TestStatus value) {
    return this == value;
  }
}
