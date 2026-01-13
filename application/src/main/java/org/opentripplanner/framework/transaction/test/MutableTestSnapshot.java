package org.opentripplanner.framework.transaction.test;

class MutableTestSnapshot {
  private String state;

  public MutableTestSnapshot(String state) {
    this.state = state;
  }

  public String state() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }
}
