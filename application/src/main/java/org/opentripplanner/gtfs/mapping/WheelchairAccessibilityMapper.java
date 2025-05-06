package org.opentripplanner.gtfs.mapping;

import org.opentripplanner.transit.model.basic.Accessibility;

class WheelchairAccessibilityMapper {

  static Accessibility map(int gtfsCode) {
    return switch (gtfsCode) {
      case 0 -> Accessibility.NO_INFORMATION;
      case 1 -> Accessibility.POSSIBLE;
      case 2 -> Accessibility.NOT_POSSIBLE;
      default -> throw new IllegalArgumentException(
        "Unknown GTFS WheelChairBoardingType: " + gtfsCode
      );
    };
  }
}
