package org.opentripplanner.apis.gtfs.mapping;

import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLAbsoluteDirection;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLRelativeDirection;
import org.opentripplanner.model.plan.AbsoluteDirection;
import org.opentripplanner.model.plan.RelativeDirection;

public final class DirectionMapper {

  public static GraphQLAbsoluteDirection map(AbsoluteDirection dir) {
    return switch (dir) {
      case NORTH -> GraphQLAbsoluteDirection.NORTH;
      case NORTHEAST -> GraphQLAbsoluteDirection.NORTHEAST;
      case EAST -> GraphQLAbsoluteDirection.EAST;
      case SOUTHEAST -> GraphQLAbsoluteDirection.SOUTHEAST;
      case SOUTH -> GraphQLAbsoluteDirection.SOUTH;
      case SOUTHWEST -> GraphQLAbsoluteDirection.SOUTHWEST;
      case WEST -> GraphQLAbsoluteDirection.WEST;
      case NORTHWEST -> GraphQLAbsoluteDirection.NORTHWEST;
    };
  }

  public static GraphQLRelativeDirection map(RelativeDirection relativeDirection) {
    return switch (relativeDirection) {
      case DEPART -> GraphQLRelativeDirection.DEPART;
      case HARD_LEFT -> GraphQLRelativeDirection.HARD_LEFT;
      case LEFT -> GraphQLRelativeDirection.LEFT;
      case SLIGHTLY_LEFT -> GraphQLRelativeDirection.SLIGHTLY_LEFT;
      case CONTINUE, ENTER_OR_EXIT_STATION -> GraphQLRelativeDirection.CONTINUE;
      case SLIGHTLY_RIGHT -> GraphQLRelativeDirection.SLIGHTLY_RIGHT;
      case RIGHT -> GraphQLRelativeDirection.RIGHT;
      case HARD_RIGHT -> GraphQLRelativeDirection.HARD_RIGHT;
      case CIRCLE_CLOCKWISE -> GraphQLRelativeDirection.CIRCLE_CLOCKWISE;
      case CIRCLE_COUNTERCLOCKWISE -> GraphQLRelativeDirection.CIRCLE_COUNTERCLOCKWISE;
      case ELEVATOR -> GraphQLRelativeDirection.ELEVATOR;
      case UTURN_LEFT -> GraphQLRelativeDirection.UTURN_LEFT;
      case UTURN_RIGHT -> GraphQLRelativeDirection.UTURN_RIGHT;
      case ENTER_STATION -> GraphQLRelativeDirection.ENTER_STATION;
      case EXIT_STATION -> GraphQLRelativeDirection.EXIT_STATION;
      case FOLLOW_SIGNS -> GraphQLRelativeDirection.FOLLOW_SIGNS;
    };
  }
}
