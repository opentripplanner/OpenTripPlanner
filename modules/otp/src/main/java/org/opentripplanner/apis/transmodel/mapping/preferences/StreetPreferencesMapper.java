package org.opentripplanner.apis.transmodel.mapping.preferences;

import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.apis.transmodel.model.framework.PenaltyForStreetModeType;
import org.opentripplanner.apis.transmodel.model.framework.StreetModeDurationInputType;
import org.opentripplanner.apis.transmodel.model.plan.TripQuery;
import org.opentripplanner.routing.api.request.preference.StreetPreferences;

public class StreetPreferencesMapper {

  public static void mapStreetPreferences(
    StreetPreferences.Builder street,
    DataFetchingEnvironment environment,
    StreetPreferences defaultPreferences
  ) {
    street.withAccessEgress(ae -> {
      ae.withPenalty(b ->
        PenaltyForStreetModeType.mapPenaltyToDomain(b, environment, TripQuery.ACCESS_EGRESS_PENALTY)
      );
      ae.withMaxDuration(builder ->
        StreetModeDurationInputType.mapDurationForStreetModeAndAssertValueIsGreaterThenDefault(
          builder,
          environment,
          TripQuery.MAX_ACCESS_EGRESS_DURATION_FOR_MODE,
          defaultPreferences.accessEgress().maxDuration()
        )
      );
    });

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
