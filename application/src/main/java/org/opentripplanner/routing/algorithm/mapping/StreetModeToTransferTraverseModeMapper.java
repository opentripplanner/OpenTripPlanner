package org.opentripplanner.routing.algorithm.mapping;

import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.street.search.TraverseMode;

/**
 * Maps street mode to transfer traverse mode.
 */
public class StreetModeToTransferTraverseModeMapper {

  public static TraverseMode map(StreetMode mode) {
    return switch (mode) {
      case WALK -> TraverseMode.WALK;
      case BIKE -> TraverseMode.BICYCLE;
      case CAR -> TraverseMode.CAR;
      default -> throw new IllegalArgumentException(
        String.format("StreetMode %s can not be mapped to a TraverseMode for transfers.", mode)
      );
    };
  }
}
