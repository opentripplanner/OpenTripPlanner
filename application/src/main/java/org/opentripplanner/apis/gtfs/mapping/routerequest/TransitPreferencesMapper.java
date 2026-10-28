package org.opentripplanner.apis.gtfs.mapping.routerequest;

import static org.opentripplanner.apis.gtfs.mapping.routerequest.ArgumentUtils.getTransitModes;

import graphql.schema.DataFetchingEnvironment;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.apis.gtfs.mapping.TransitModeMapper;
import org.opentripplanner.apis.support.InvalidInputException;
import org.opentripplanner.core.model.basic.Cost;
import org.opentripplanner.routing.api.request.framework.CostLinearFunction;
import org.opentripplanner.routing.api.request.preference.TransferPreferences;
import org.opentripplanner.routing.api.request.preference.TransitPreferences;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.utils.collection.CollectionUtils;
import org.opentripplanner.utils.time.DurationUtils;

public class TransitPreferencesMapper {

  static void setTransitPreferences(
    TransitPreferences.Builder transitPreferences,
    TransferPreferences.Builder transferPreferences,
    GraphQLTypes.GraphQLQueryTypePlanConnectionArgs args,
    DataFetchingEnvironment environment
  ) {
    var modes = args.getGraphQLModes();
    var transit = getTransitModes(environment);
    if (!Boolean.TRUE.equals(modes.getGraphQLDirectOnly()) && !CollectionUtils.isEmpty(transit)) {
      transitPreferences.withReluctanceForMode(mapReluctanceForMode(transit));
    }
    var transitArgs = args.getGraphQLPreferences().getGraphQLTransit();
    if (transitArgs == null) {
      return;
    }

    var relax = transitArgs.getGraphQLRelaxTransitGroupPriority();
    if (relax != null) {
      Cost constant = Cost.ZERO;
      Double coefficient = 1.0;
      if (relax.getGraphQLConstant() != null) {
        constant = relax.getGraphQLConstant();
      }
      if (relax.getGraphQLCoefficient() != null) {
        coefficient = relax.getGraphQLCoefficient();
      }
      transitPreferences.withRelaxTransitGroupPriority(
        CostLinearFunction.of(constant, coefficient)
      );
    }

    var board = transitArgs.getGraphQLBoard();
    if (board != null) {
      var slack = board.getGraphQLSlack();
      if (slack != null) {
        transitPreferences.withDefaultBoardSlackSec(
          (int) DurationUtils.requireNonNegativeMax2hours(slack, "board slack").toSeconds()
        );
      }
      var waitReluctance = board.getGraphQLWaitReluctance();
      if (waitReluctance != null) {
        transferPreferences.withWaitReluctance(waitReluctance);
      }
    }
    var alight = transitArgs.getGraphQLAlight();
    if (alight != null) {
      var slack = alight.getGraphQLSlack();
      if (slack != null) {
        transitPreferences.withDefaultAlightSlackSec(
          (int) DurationUtils.requireNonNegativeMax2hours(slack, "alight slack").toSeconds()
        );
      }
    }
    var transfer = transitArgs.getGraphQLTransfer();
    if (transfer != null) {
      var cost = transfer.getGraphQLCost();
      if (cost != null) {
        transferPreferences.withCost(cost.toSeconds());
      }
      var slack = transfer.getGraphQLSlack();
      if (slack != null) {
        transferPreferences.withSlack(
          DurationUtils.requireNonNegativeMax2hours(slack, "transfer slack")
        );
      }
      var maxTransfers = transfer.getGraphQLMaximumTransfers();
      if (maxTransfers != null) {
        if (maxTransfers < 0) {
          throw new InvalidInputException("Maximum transfers must be non-negative.");
        }
        transferPreferences.withMaxTransfers(maxTransfers + 1);
      }
      var additionalTransfers = transfer.getGraphQLMaximumAdditionalTransfers();
      if (additionalTransfers != null) {
        if (additionalTransfers < 0) {
          throw new InvalidInputException("Maximum additional transfers must be non-negative.");
        }
        transferPreferences.withMaxAdditionalTransfers(additionalTransfers);
      }
    }
    var timetable = transitArgs.getGraphQLTimetable();
    if (timetable != null) {
      var excludeUpdates = timetable.getGraphQLExcludeRealTimeUpdates();
      if (excludeUpdates != null) {
        transitPreferences.withIgnoreRealtimeUpdates(excludeUpdates);
      }
      var includePlannedCancellations = timetable.getGraphQLIncludePlannedCancellations();
      if (includePlannedCancellations != null) {
        transitPreferences.withIncludePlannedCancellations(includePlannedCancellations);
      }
      var includeRealtimeCancellations = timetable.getGraphQLIncludeRealTimeCancellations();
      if (includeRealtimeCancellations != null) {
        transitPreferences.withIncludeRealtimeCancellations(includeRealtimeCancellations);
      }
    }
  }

  /**
   * The reluctance is defined per main mode, so a mode listed more than once, e.g. with different
   * geographical scopes, must have the same reluctance in all entries that set one.
   */
  private static Map<TransitMode, Double> mapReluctanceForMode(List<Map<String, Object>> transit) {
    var reluctanceForMode = new EnumMap<TransitMode, Double>(TransitMode.class);
    for (var mode : transit) {
      if (!mode.containsKey("cost")) {
        continue;
      }
      var transitMode = TransitModeMapper.map(
        GraphQLTypes.GraphQLTransitMode.valueOf((String) mode.get("mode"))
      );
      var reluctance = (Double) ((Map<String, Object>) mode.get("cost")).get("reluctance");
      var previous = reluctanceForMode.putIfAbsent(transitMode, reluctance);
      if (previous != null && !previous.equals(reluctance)) {
        throw new InvalidInputException(
          "Transit mode %s is listed with different reluctances (%s and %s). The reluctance can only be set per transit mode, not per geographical scope or replacement.".formatted(
            transitMode,
            previous,
            reluctance
          )
        );
      }
    }
    return reluctanceForMode;
  }
}
