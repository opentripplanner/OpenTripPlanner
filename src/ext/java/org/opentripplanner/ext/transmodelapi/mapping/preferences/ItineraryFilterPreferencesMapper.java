package org.opentripplanner.ext.transmodelapi.mapping.preferences;

import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.transmodelapi.model.plan.ItineraryFiltersInputType;
import org.opentripplanner.ext.transmodelapi.support.DataFetcherDecorator;
import org.opentripplanner.routing.api.request.preference.ItineraryFilterDebugProfile;
import org.opentripplanner.routing.api.request.preference.ItineraryFilterPreferences;

public class ItineraryFilterPreferencesMapper {

  public static void mapItineraryFilterPreferences(
    ItineraryFilterPreferences.Builder itineraryFilter,
    DataFetchingEnvironment environment,
    DataFetcherDecorator callWith
  ) {
    callWith.argument(
      "debugItineraryFilter",
      (Boolean v) -> itineraryFilter.withDebug(ItineraryFilterDebugProfile.ofDebugEnabled(v))
    );
    ItineraryFiltersInputType.mapToRequest(environment, callWith, itineraryFilter);
  }
}
