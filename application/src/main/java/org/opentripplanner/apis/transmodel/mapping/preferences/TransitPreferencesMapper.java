package org.opentripplanner.apis.transmodel.mapping.preferences;

import graphql.schema.DataFetchingEnvironment;
import java.util.Map;
import org.opentripplanner.apis.transmodel.model.TransportModeSlack;
import org.opentripplanner.apis.transmodel.model.plan.RelaxCostType;
import org.opentripplanner.apis.transmodel.support.DataFetcherDecorator;
import org.opentripplanner.routing.api.request.framework.CostLinearFunction;
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
      callWith.argument("boardSlackList", (Object v) -> TransportModeSlack.mapIntoDomain(builder, v)
      );
    });
    transit.withAlightSlack(builder -> {
      callWith.argument("alightSlackDefault", builder::withDefaultSec);
      callWith.argument("alightSlackList", (Object v) ->
        TransportModeSlack.mapIntoDomain(builder, v)
      );
    });
    callWith.argument("ignoreRealtimeUpdates", transit::setIgnoreRealtimeUpdates);
    callWith.argument("includePlannedCancellations", transit::setIncludePlannedCancellations);
    callWith.argument("includeRealtimeCancellations", transit::setIncludeRealtimeCancellations);
    callWith.argument("relaxTransitGroupPriority", it ->
      transit.withRelaxTransitGroupPriority(
        RelaxCostType.mapToDomain((Map<String, Object>) it, CostLinearFunction.NORMAL)
      )
    );
    callWith.argument("relaxTransitSearchGeneralizedCostAtDestination", (Double value) ->
      transit.withRaptor(it -> it.withRelaxGeneralizedCostAtDestination(value))
    );
  }
}
