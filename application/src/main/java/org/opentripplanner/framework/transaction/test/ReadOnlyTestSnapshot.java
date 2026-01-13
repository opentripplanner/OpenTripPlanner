package org.opentripplanner.framework.transaction.test;

class ReadOnlyTestSnapshot {
  private final String state;

  public String state() {
    return state;
  }

  public ReadOnlyTestSnapshot(String state) {
    this.state = state;
  }
}
