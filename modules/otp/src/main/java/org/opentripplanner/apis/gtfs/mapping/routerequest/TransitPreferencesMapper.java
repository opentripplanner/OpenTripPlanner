package org.opentripplanner.apis.gtfs.mapping.routerequest;

import static org.opentripplanner.apis.gtfs.mapping.routerequest.ArgumentUtils.getTransitModes;

import graphql.schema.DataFetchingEnvironment;
import java.util.Map;
import java.util.stream.Collectors;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.apis.gtfs.mapping.TransitModeMapper;
import org.opentripplanner.framework.collection.CollectionUtils;
import org.opentripplanner.framework.time.DurationUtils;
import org.opentripplanner.routing.api.request.preference.TransferPreferences;
import org.opentripplanner.routing.api.request.preference.TransitPreferences;

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
      var reluctanceForMode = transit
        .stream()
        .filter(mode -> mode.containsKey("cost"))
        .collect(
          Collectors.toMap(
            mode ->
              TransitModeMapper.map(
                GraphQLTypes.GraphQLTransitMode.valueOf((String) mode.get("mode"))
              ),
            mode -> (Double) ((Map<String, Object>) mode.get("cost")).get("reluctance")
          )
        );
      transitPreferences.setReluctanceForMode(reluctanceForMode);
    }
    var transitArgs = args.getGraphQLPreferences().getGraphQLTransit();
    if (transitArgs == null) {
      return;
    }

    var board = transitArgs.getGraphQLBoard();
    if (board != null) {
      var slack = board.getGraphQLSlack();
      if (slack != null) {
        transitPreferences.withDefaultBoardSlackSec(
          (int) DurationUtils.requireNonNegativeMedium(slack, "board slack").toSeconds()
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
          (int) DurationUtils.requireNonNegativeMedium(slack, "alight slack").toSeconds()
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
          DurationUtils.requireNonNegativeMedium(slack, "transfer slack")
        );
      }
      var maxTransfers = transfer.getGraphQLMaximumTransfers();
      if (maxTransfers != null) {
        if (maxTransfers < 0) {
          throw new IllegalArgumentException("Maximum transfers must be non-negative.");
        }
        transferPreferences.withMaxTransfers(maxTransfers + 1);
      }
      var additionalTransfers = transfer.getGraphQLMaximumAdditionalTransfers();
      if (additionalTransfers != null) {
        if (additionalTransfers < 0) {
          throw new IllegalArgumentException("Maximum additional transfers must be non-negative.");
        }
        transferPreferences.withMaxAdditionalTransfers(additionalTransfers);
      }
    }
    var timetable = transitArgs.getGraphQLTimetable();
    if (timetable != null) {
      var excludeUpdates = timetable.getGraphQLExcludeRealTimeUpdates();
      if (excludeUpdates != null) {
        transitPreferences.setIgnoreRealtimeUpdates(excludeUpdates);
      }
      var includePlannedCancellations = timetable.getGraphQLIncludePlannedCancellations();
      if (includePlannedCancellations != null) {
        transitPreferences.setIncludePlannedCancellations(includePlannedCancellations);
      }
      var includeRealtimeCancellations = timetable.getGraphQLIncludeRealTimeCancellations();
      if (includeRealtimeCancellations != null) {
        transitPreferences.setIncludeRealtimeCancellations(includeRealtimeCancellations);
      }
    }
  }
}
