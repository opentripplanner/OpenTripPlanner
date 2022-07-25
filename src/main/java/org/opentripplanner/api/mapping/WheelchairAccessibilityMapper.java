package org.opentripplanner.api.mapping;

import org.opentripplanner.transit.model.basic.WheelchairAccessibility;

public class WheelchairAccessibilityMapper {

  public static Integer mapToApi(WheelchairAccessibility domain) {
    if (domain == null) {
      return 0;
    }

    return switch (domain) {
      case NO_INFORMATION -> 0;
      case POSSIBLE -> 1;
      case NOT_POSSIBLE -> 2;
    };
  }
}
