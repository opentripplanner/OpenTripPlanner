package org.opentripplanner.model;

public enum WheelchairAccessibility {
  NO_INFORMATION(0),
  POSSIBLE(1),
  NOT_POSSIBLE(2);

  public final int gtfsCode;

  WheelchairAccessibility(int gtfsCode) {
    this.gtfsCode = gtfsCode;
  }

  public static WheelchairAccessibility valueOfGtfsCode(int gtfsCode) {
    for (WheelchairAccessibility value : values()) {
      if (value.gtfsCode == gtfsCode) {
        return value;
      }
    }
    throw new IllegalArgumentException("Unknown GTFS WheelChairBoardingType: " + gtfsCode);
  }
}
