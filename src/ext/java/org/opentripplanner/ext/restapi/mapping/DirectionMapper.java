package org.opentripplanner.ext.restapi.mapping;

import org.opentripplanner.transit.model.timetable.Direction;

public class DirectionMapper {

  public static Integer mapToApi(Direction direction) {
    if (direction == Direction.UNKNOWN) {
      return null;
    }

    return direction.gtfsCode;
  }
}
