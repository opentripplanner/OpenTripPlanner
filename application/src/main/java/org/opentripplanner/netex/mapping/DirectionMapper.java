package org.opentripplanner.netex.mapping;

import org.entur.netex2.model.DirectionTypeEnumeration;
import org.opentripplanner.transit.model.timetable.Direction;

class DirectionMapper {

  static Direction map(DirectionTypeEnumeration direction) {
    if (direction == null) {
      return Direction.UNKNOWN;
    }
    return switch (direction) {
      case INBOUND -> Direction.INBOUND;
      case OUTBOUND -> Direction.OUTBOUND;
      case CLOCKWISE -> Direction.CLOCKWISE;
      case ANTICLOCKWISE -> Direction.ANTICLOCKWISE;
    };
  }
}
