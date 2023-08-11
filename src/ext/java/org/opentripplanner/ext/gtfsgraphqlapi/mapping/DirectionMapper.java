package org.opentripplanner.ext.gtfsgraphqlapi.mapping;

import javax.annotation.Nonnull;
import org.opentripplanner.ext.gtfsgraphqlapi.generated.GraphQLTypes;
import org.opentripplanner.model.plan.AbsoluteDirection;
import org.opentripplanner.model.plan.RelativeDirection;

public class DirectionMapper {

  @Nonnull
  public static GraphQLTypes.GraphQLAbsoluteDirection map(AbsoluteDirection dir) {
    return switch (dir) {
      case NORTH -> GraphQLTypes.GraphQLAbsoluteDirection.NORTH;
      case NORTHEAST -> GraphQLTypes.GraphQLAbsoluteDirection.NORTHEAST;
      case EAST -> GraphQLTypes.GraphQLAbsoluteDirection.EAST;
      case SOUTHEAST -> GraphQLTypes.GraphQLAbsoluteDirection.SOUTHEAST;
      case SOUTH -> GraphQLTypes.GraphQLAbsoluteDirection.SOUTH;
      case SOUTHWEST -> GraphQLTypes.GraphQLAbsoluteDirection.SOUTHWEST;
      case WEST -> GraphQLTypes.GraphQLAbsoluteDirection.WEST;
      case NORTHWEST -> GraphQLTypes.GraphQLAbsoluteDirection.NORTHWEST;
    };
  }

  @Nonnull
  public static GraphQLTypes.GraphQLRelativeDirection map(RelativeDirection relativeDirection) {
    return switch (relativeDirection) {
      case DEPART -> GraphQLTypes.GraphQLRelativeDirection.DEPART;
      case HARD_LEFT -> GraphQLTypes.GraphQLRelativeDirection.HARD_LEFT;
      case LEFT -> GraphQLTypes.GraphQLRelativeDirection.LEFT;
      case SLIGHTLY_LEFT -> GraphQLTypes.GraphQLRelativeDirection.SLIGHTLY_LEFT;
      case CONTINUE -> GraphQLTypes.GraphQLRelativeDirection.CONTINUE;
      case SLIGHTLY_RIGHT -> GraphQLTypes.GraphQLRelativeDirection.SLIGHTLY_RIGHT;
      case RIGHT -> GraphQLTypes.GraphQLRelativeDirection.RIGHT;
      case HARD_RIGHT -> GraphQLTypes.GraphQLRelativeDirection.HARD_RIGHT;
      case CIRCLE_CLOCKWISE -> GraphQLTypes.GraphQLRelativeDirection.CIRCLE_CLOCKWISE;
      case CIRCLE_COUNTERCLOCKWISE -> GraphQLTypes.GraphQLRelativeDirection.CIRCLE_COUNTERCLOCKWISE;
      case ELEVATOR -> GraphQLTypes.GraphQLRelativeDirection.ELEVATOR;
      case UTURN_LEFT -> GraphQLTypes.GraphQLRelativeDirection.UTURN_LEFT;
      case UTURN_RIGHT -> GraphQLTypes.GraphQLRelativeDirection.UTURN_RIGHT;
      case ENTER_STATION -> GraphQLTypes.GraphQLRelativeDirection.ENTER_STATION;
      case EXIT_STATION -> GraphQLTypes.GraphQLRelativeDirection.EXIT_STATION;
      case FOLLOW_SIGNS -> GraphQLTypes.GraphQLRelativeDirection.FOLLOW_SIGNS;
    };
  }
}
