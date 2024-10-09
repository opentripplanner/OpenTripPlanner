package org.opentripplanner.model;

public enum PickDrop {
  SCHEDULED(true),
  NONE(false),
  CALL_AGENCY(true),
  COORDINATE_WITH_DRIVER(true),
  CANCELLED(false);

  private final boolean routable;

  PickDrop(boolean routable) {
    this.routable = routable;
  }

  public boolean is(PickDrop value) {
    return this == value;
  }

  public boolean isRoutable() {
    return routable;
  }

  public boolean isNotRoutable() {
    return !routable;
  }
}
