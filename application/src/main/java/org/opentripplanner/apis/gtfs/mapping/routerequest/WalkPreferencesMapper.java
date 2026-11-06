package org.opentripplanner.apis.gtfs.mapping.routerequest;

import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.routing.api.request.preference.WalkPreferences;
import org.opentripplanner.routing.api.request.preference.WheelchairPreferences;

public class WalkPreferencesMapper {

  static void setWalkPreferences(
    WalkPreferences.Builder preferences,
    GraphQLTypes.GraphQLWalkPreferencesInput args
  ) {
    if (args == null) {
      return;
    }

    var speed = args.getGraphQLSpeed();
    if (speed != null) {
      preferences.withSpeed(speed);
    }
    var reluctance = args.getGraphQLReluctance();
    if (reluctance != null) {
      preferences.withReluctance(reluctance);
    }
    var walkSafetyFactor = args.getGraphQLSafetyFactor();
    if (walkSafetyFactor != null) {
      preferences.withSafetyFactor(walkSafetyFactor);
    }
    var boardCost = args.getGraphQLBoardCost();
    if (boardCost != null) {
      preferences.withBoardCost(boardCost.toSeconds());
    }
  }

  /// The API has no separate wheelchair parameters, the walk values apply to wheelchair users
  /// as well.
  static void setWheelchairWalkPreferences(
    WheelchairPreferences.Builder preferences,
    GraphQLTypes.GraphQLWalkPreferencesInput args
  ) {
    if (args == null) {
      return;
    }

    var speed = args.getGraphQLSpeed();
    if (speed != null) {
      preferences.withSpeed(speed);
    }
    var reluctance = args.getGraphQLReluctance();
    if (reluctance != null) {
      preferences.withReluctance(reluctance);
    }
  }
}
