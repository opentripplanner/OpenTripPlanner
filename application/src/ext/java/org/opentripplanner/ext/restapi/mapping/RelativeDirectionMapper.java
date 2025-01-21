package org.opentripplanner.ext.restapi.mapping;

import org.opentripplanner.ext.restapi.model.ApiRelativeDirection;
import org.opentripplanner.model.plan.RelativeDirection;

public class RelativeDirectionMapper {

  public static ApiRelativeDirection mapRelativeDirection(RelativeDirection domain) {
    if (domain == null) {
      return null;
    }
    return switch (domain) {
      case DEPART -> ApiRelativeDirection.DEPART;
      case HARD_LEFT -> ApiRelativeDirection.HARD_LEFT;
      case LEFT -> ApiRelativeDirection.LEFT;
      case SLIGHTLY_LEFT -> ApiRelativeDirection.SLIGHTLY_LEFT;
      case CONTINUE, ENTER_OR_EXIT_STATION -> ApiRelativeDirection.CONTINUE;
      case SLIGHTLY_RIGHT -> ApiRelativeDirection.SLIGHTLY_RIGHT;
      case RIGHT -> ApiRelativeDirection.RIGHT;
      case HARD_RIGHT -> ApiRelativeDirection.HARD_RIGHT;
      case CIRCLE_CLOCKWISE -> ApiRelativeDirection.CIRCLE_CLOCKWISE;
      case CIRCLE_COUNTERCLOCKWISE -> ApiRelativeDirection.CIRCLE_COUNTERCLOCKWISE;
      case ELEVATOR -> ApiRelativeDirection.ELEVATOR;
      case UTURN_LEFT -> ApiRelativeDirection.UTURN_LEFT;
      case UTURN_RIGHT -> ApiRelativeDirection.UTURN_RIGHT;
      case ENTER_STATION -> ApiRelativeDirection.ENTER_STATION;
      case EXIT_STATION -> ApiRelativeDirection.EXIT_STATION;
      case FOLLOW_SIGNS -> ApiRelativeDirection.FOLLOW_SIGNS;
    };
  }
}
