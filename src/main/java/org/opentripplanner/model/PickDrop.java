package org.opentripplanner.model;

public enum PickDrop {
  SCHEDULED(true, 0),
  NONE(false, 1),
  CALL_AGENCY(true, 2),
  COORDINATE_WITH_DRIVER(true, 3),
  CANCELLED(false, -1);

  private final boolean routable;

  private final int gtfsCode;

  PickDrop(boolean routable, int gtfsCode) {
    this.routable = routable;
    this.gtfsCode = gtfsCode;
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

  public int getGtfsCode() {
    return gtfsCode;
  }

  public static PickDrop fromGtfsCode(int gtfsCode) {
    for (PickDrop pickDrop : PickDrop.values()) {
      if (pickDrop.gtfsCode == gtfsCode) return pickDrop;
    }
    throw new IllegalArgumentException("Not a valid gtfs code: " + gtfsCode);
  }
}
