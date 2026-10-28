package org.opentripplanner.apis.transmodel.mapping.preferences;

import org.opentripplanner.apis.transmodel.support.DataFetcherDecorator;
import org.opentripplanner.routing.api.request.preference.WalkPreferences;
import org.opentripplanner.routing.api.request.preference.WheelchairPreferences;

public class WalkPreferencesMapper {

  public static void mapWalkPreferences(
    WalkPreferences.Builder walk,
    DataFetcherDecorator callWith
  ) {
    // This is not part of API
    // callWith.argument("walkBoardCost", walk::withBoardCost);
    callWith.argument("walkSpeed", walk::withSpeed);
    callWith.argument("walkReluctance", walk::withReluctance);
  }

  /// The API has no separate wheelchair parameters, the walk values apply to wheelchair users
  /// as well.
  public static void mapWheelchairPreferences(
    WheelchairPreferences.Builder wheelchair,
    DataFetcherDecorator callWith
  ) {
    callWith.argument("walkSpeed", wheelchair::withSpeed);
    callWith.argument("walkReluctance", wheelchair::withReluctance);
  }
}
