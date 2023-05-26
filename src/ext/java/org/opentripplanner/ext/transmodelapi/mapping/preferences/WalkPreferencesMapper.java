package org.opentripplanner.ext.transmodelapi.mapping.preferences;

import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.transmodelapi.support.DataFetcherDecorator;
import org.opentripplanner.routing.api.request.preference.WalkPreferences;

public class WalkPreferencesMapper {

  public static void mapWalkPreferences(
    WalkPreferences.Builder walk,
    DataFetchingEnvironment environment,
    DataFetcherDecorator callWith
  ) {
    callWith.argument("walkBoardCost", walk::withBoardCost);
    callWith.argument("walkSpeed", walk::withSpeed);
  }
}
