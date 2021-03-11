package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.Direction;
import org.rutebanken.netex.model.DirectionTypeEnumeration;

class DirectionMapper {
  static Direction map(DirectionTypeEnumeration direction) {
    if (direction == null) { return Direction.UNKNOWN; }
    switch (direction) {
      case INBOUND:
        return Direction.INBOUND;
      case OUTBOUND:
        return Direction.OUTBOUND;
      case CLOCKWISE:
        return Direction.CLOCKWISE;
      case ANTICLOCKWISE:
        return Direction.ANTICLOCKWISE;
      default:
        return Direction.UNKNOWN;
    }
  }
}
