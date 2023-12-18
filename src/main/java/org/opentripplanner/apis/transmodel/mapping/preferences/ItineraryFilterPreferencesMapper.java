package org.opentripplanner.apis.transmodel.mapping.preferences;

import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.apis.transmodel.model.plan.ItineraryFiltersInputType;
import org.opentripplanner.apis.transmodel.support.DataFetcherDecorator;
import org.opentripplanner.routing.api.request.preference.ItineraryFilterDebugProfile;
import org.opentripplanner.routing.api.request.preference.ItineraryFilterPreferences;
import org.opentripplanner.routing.api.request.preference.VehicleRentalPreferences;

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

  public static void mapRentalPreferences(
    VehicleRentalPreferences.Builder rental,
    DataFetchingEnvironment environment,
    DataFetcherDecorator callWith
  ) {
    callWith.argument(
      "useBikeRentalAvailabilityInformation",
      rental::withUseAvailabilityInformation
    );
  }
}
