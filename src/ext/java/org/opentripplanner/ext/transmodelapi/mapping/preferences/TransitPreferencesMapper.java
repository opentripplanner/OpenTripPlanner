package org.opentripplanner.ext.transmodelapi.mapping.preferences;

import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.transmodelapi.model.TransportModeSlack;
import org.opentripplanner.ext.transmodelapi.support.DataFetcherDecorator;
import org.opentripplanner.routing.api.request.preference.TransitPreferences;

public class TransitPreferencesMapper {

  public static void mapTransitPreferences(
    TransitPreferences.Builder transit,
    DataFetchingEnvironment environment,
    DataFetcherDecorator callWith
  ) {
    callWith.argument(
      "preferred.otherThanPreferredLinesPenalty",
      transit::setOtherThanPreferredRoutesPenalty
    );
    transit.withBoardSlack(builder -> {
      callWith.argument("boardSlackDefault", builder::withDefaultSec);
      callWith.argument(
        "boardSlackList",
        (Integer v) -> TransportModeSlack.mapIntoDomain(builder, v)
      );
    });
    transit.withAlightSlack(builder -> {
      callWith.argument("alightSlackDefault", builder::withDefaultSec);
      callWith.argument(
        "alightSlackList",
        (Object v) -> TransportModeSlack.mapIntoDomain(builder, v)
      );
    });
    callWith.argument("ignoreRealtimeUpdates", transit::setIgnoreRealtimeUpdates);
    callWith.argument("includePlannedCancellations", transit::setIncludePlannedCancellations);
    callWith.argument("includeRealtimeCancellations", transit::setIncludeRealtimeCancellations);
    callWith.argument(
      "relaxTransitSearchGeneralizedCostAtDestination",
      (Double value) -> transit.withRaptor(it -> it.withRelaxGeneralizedCostAtDestination(value))
    );
  }
}
