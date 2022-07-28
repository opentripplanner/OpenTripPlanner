package org.opentripplanner.gtfs.mapping;

import org.opentripplanner.transit.model.basic.WheelchairAccessibility;

public class WheelchairAccessibilityMapper {

  static WheelchairAccessibility map(int gtfsCode) {
    return switch (gtfsCode) {
      case 0 -> WheelchairAccessibility.NO_INFORMATION;
      case 1 -> WheelchairAccessibility.POSSIBLE;
      case 2 -> WheelchairAccessibility.NOT_POSSIBLE;
      default -> throw new IllegalArgumentException(
        "Unknown GTFS WheelChairBoardingType: " + gtfsCode
      );
    };
  }
}
