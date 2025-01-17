package org.opentripplanner.apis.transmodel.mapping;

import org.opentripplanner.model.plan.RelativeDirection;

/**
 * This mapper makes sure that only those values are returned which have a mapping in the Transmodel API,
 * as we don't really want to return all of them.
 */
public class RelativeDirectionMapper {

  public static RelativeDirection map(RelativeDirection relativeDirection) {
    return switch (relativeDirection) {
      case DEPART,
        SLIGHTLY_LEFT,
        HARD_LEFT,
        LEFT,
        CONTINUE,
        SLIGHTLY_RIGHT,
        RIGHT,
        HARD_RIGHT,
        CIRCLE_CLOCKWISE,
        CIRCLE_COUNTERCLOCKWISE,
        ELEVATOR,
        UTURN_LEFT,
        UTURN_RIGHT -> relativeDirection;
      // for these the Transmodel API doesn't have a mapping. should it?
      case ENTER_STATION,
        EXIT_STATION,
        ENTER_OR_EXIT_STATION,
        FOLLOW_SIGNS -> RelativeDirection.CONTINUE;
    };
  }
}
