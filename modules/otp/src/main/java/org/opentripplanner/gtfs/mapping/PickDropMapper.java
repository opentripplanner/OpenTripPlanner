package org.opentripplanner.gtfs.mapping;

import static org.opentripplanner.model.StopTime.MISSING_VALUE;

import org.opentripplanner.model.PickDrop;

public class PickDropMapper {

  public static PickDrop map(int gtfsCode) {
    return switch (gtfsCode) {
      case 0 -> PickDrop.SCHEDULED;
      case 1 -> PickDrop.NONE;
      case 2 -> PickDrop.CALL_AGENCY;
      case 3 -> PickDrop.COORDINATE_WITH_DRIVER;
      default -> throw new IllegalArgumentException("Not a valid gtfs code: " + gtfsCode);
    };
  }

  public static PickDrop mapFlexContinuousPickDrop(int gtfsCode) {
    if (gtfsCode == MISSING_VALUE) {
      return PickDrop.NONE;
    }
    return map(gtfsCode);
  }
}
