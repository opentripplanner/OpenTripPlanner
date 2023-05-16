package org.opentripplanner.ext.transmodelapi.mapping;

import graphql.schema.DataFetchingEnvironment;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.opentripplanner.ext.transmodelapi.model.TransportModeSlack;
import org.opentripplanner.ext.transmodelapi.model.plan.ItineraryFiltersInputType;
import org.opentripplanner.ext.transmodelapi.support.DataFetcherDecorator;
import org.opentripplanner.ext.transmodelapi.support.GqlUtil;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.preference.ItineraryFilterDebugProfile;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.routing.core.BicycleOptimizeType;

class PreferencesMapper {

  static void mapPreferences(
    DataFetchingEnvironment environment,
    DataFetcherDecorator callWith,
    RoutingPreferences.Builder preferences
  ) {
    preferences.withWalk(walk -> mapWalkPreferences(walk, environment, callWith));
    preferences.withStreet(street -> mapStreetPreferences(street, environment, preferences.street())
    );
    preferences.withBike(bike -> mapBikePreferences(bike, environment, callWith));
    preferences.withTransfer(transfer -> mapTransferPreferences(transfer, environment, callWith));
    preferences.withTransit(transit -> mapTransitPreferences(transit, environment, callWith));
    preferences.withItineraryFilter(itineraryFilter ->
      mapItineraryFilterPreferences(itineraryFilter, environment, callWith)
    );
    preferences.withRental(rental -> mapRentalPreferences(rental, environment, callWith));
  }

  @SuppressWarnings("unchecked")
  private static void mapWalkPreferences(
    WalkPreferences.Builder walk,
    DataFetchingEnvironment environment,
    DataFetcherDecorator callWith
  ) {
    callWith.argument("walkBoardCost", walk::withBoardCost);
    callWith.argument("walkSpeed", walk::withSpeed);
  }

  @SuppressWarnings("unchecked")
  private static void mapStreetPreferences(
    StreetPreferences.Builder street,
    DataFetchingEnvironment environment,
    StreetPreferences defaultPreferences
  ) {
    if (GqlUtil.hasArgument(environment, "maxAccessEgressDurationForMode")) {
      for (var entry : (List<Map<String, ?>>) environment.getArgument(
        "maxAccessEgressDurationForMode"
      )) {
        var streetMode = (StreetMode) entry.get("streetMode");
        var duration = (Duration) entry.get("duration");

        var defaultDuration = defaultPreferences.maxAccessEgressDuration().valueOf(streetMode);

        if (defaultDuration.compareTo(duration) < 0) {
          throw new IllegalArgumentException(
            "Invalid max access/egress duration for mode " + streetMode
          );
        }

        street.withMaxAccessEgressDuration(streetMode, duration);
      }
    }

    if (GqlUtil.hasArgument(environment, "maxDirectDurationForMode")) {
      for (var entry : (List<Map<String, ?>>) environment.getArgument("maxDirectDurationForMode")) {
        var streetMode = (StreetMode) entry.get("streetMode");
        var duration = (Duration) entry.get("duration");

        var defaultDuration = defaultPreferences.maxDirectDuration().defaultValue();

        if (defaultDuration.compareTo(duration) < 0) {
          throw new IllegalArgumentException("Invalid max direct duration for mode " + streetMode);
        }

        street.withMaxDirectDuration(streetMode, duration);
      }
    }
  }

  private static void mapBikePreferences(
    BikePreferences.Builder bike,
    DataFetchingEnvironment environment,
    DataFetcherDecorator callWith
  ) {
    callWith.argument("bikeSpeed", bike::withSpeed);
    callWith.argument("bikeSwitchTime", bike::withSwitchTime);
    callWith.argument("bikeSwitchCost", bike::withSwitchCost);
    callWith.argument("bicycleOptimisationMethod", bike::withOptimizeType);

    if (bike.optimizeType() == BicycleOptimizeType.TRIANGLE) {
      bike.withOptimizeTriangle(triangle -> {
        callWith.argument("triangle.timeFactor", triangle::withTime);
        callWith.argument("triangle.slopeFactor", triangle::withSlope);
        callWith.argument("triangle.safetyFactor", triangle::withSafety);
      });
    }
  }

  private static void mapTransferPreferences(
    TransferPreferences.Builder transfer,
    DataFetchingEnvironment environment,
    DataFetcherDecorator callWith
  ) {
    callWith.argument("transferPenalty", transfer::withCost);

    // 'minimumTransferTime' is deprecated, that's why we are mapping 'slack' twice.
    callWith.argument("minimumTransferTime", transfer::withSlack);
    callWith.argument("transferSlack", transfer::withSlack);

    callWith.argument("waitReluctance", transfer::withWaitReluctance);
    callWith.argument("maximumTransfers", transfer::withMaxTransfers);
    callWith.argument("maximumAdditionalTransfers", transfer::withMaxAdditionalTransfers);
  }

  private static void mapTransitPreferences(
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

  private static void mapItineraryFilterPreferences(
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

  private static void mapRentalPreferences(
    VehicleRentalPreferences.Builder rental,
    DataFetchingEnvironment environment,
    DataFetcherDecorator callWith
  ) {
    callWith.argument(
      "useBikeRentalAvailabilityInformation",
      rental::withUseAvailabilityInformation
    );
  }

  /**
   * This set the reluctance for bike, walk, car and bikeWalking (x2.7) - all in one go. These
   * parameters can be set individually.
   */
  private static void setStreetReluctance(
    RoutingPreferences.Builder preferences,
    Double streetReluctance
  ) {
    if (streetReluctance > 0) {
      preferences.withWalk(walk -> walk.withReluctance(streetReluctance));
      preferences.withBike(bike ->
        bike.withReluctance(streetReluctance).withWalkingReluctance(streetReluctance * 2.7)
      );
      preferences.withCar(car -> car.withReluctance(streetReluctance));
    }
  }
}
