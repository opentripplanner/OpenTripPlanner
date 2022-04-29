package org.opentripplanner.model;

public enum WheelchairBoarding {
  NO_INFORMATION(0),
  POSSIBLE(1),
  NOT_POSSIBLE(2);

  public final int gtfsCode;

  WheelchairBoarding(int gtfsCode) {
    this.gtfsCode = gtfsCode;
  }

  public static WheelchairBoarding valueOfGtfsCode(int gtfsCode) {
    for (WheelchairBoarding value : values()) {
      if (value.gtfsCode == gtfsCode) {
        return value;
      }
    }
    throw new IllegalArgumentException("Unknown GTFS WheelChairBoardingType: " + gtfsCode);
  }
}
