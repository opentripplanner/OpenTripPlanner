package org.opentripplanner.apis.transmodel.mapping.preferences;

import org.opentripplanner.apis.transmodel.support.DataFetcherDecorator;
import org.opentripplanner.routing.api.request.preference.WalkPreferences;

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
}
