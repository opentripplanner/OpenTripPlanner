package org.opentripplanner.ext.transmodelapi.mapping.preferences;

import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.transmodelapi.model.framework.StreetModeDurationInputType;
import org.opentripplanner.ext.transmodelapi.model.plan.TripQuery;
import org.opentripplanner.routing.api.request.preference.StreetPreferences;

public class StreetPreferencesMapper {

  public static void mapStreetPreferences(
    StreetPreferences.Builder street,
    DataFetchingEnvironment environment,
    StreetPreferences defaultPreferences
  ) {
    street.withMaxAccessEgressDuration(builder ->
      StreetModeDurationInputType.mapDurationForStreetModeAndAssertValueIsGreaterThenDefault(
        builder,
        environment,
        TripQuery.MAX_ACCESS_EGRESS_DURATION_FOR_MODE,
        defaultPreferences.maxAccessEgressDuration()
      )
    );

    street.withMaxDirectDuration(builder ->
      StreetModeDurationInputType.mapDurationForStreetModeAndAssertValueIsGreaterThenDefault(
        builder,
        environment,
        TripQuery.MAX_DIRECT_DURATION_FOR_MODE,
        defaultPreferences.maxDirectDuration()
      )
    );
  }
}
