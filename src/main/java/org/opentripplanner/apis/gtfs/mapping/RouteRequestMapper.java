package org.opentripplanner.apis.gtfs.mapping;

import graphql.schema.DataFetchingEnvironment;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opentripplanner.apis.gtfs.GraphQLRequestContext;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.framework.graphql.GraphQLUtils;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.preference.ItineraryFilterPreferences;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.routing.api.request.preference.TransitPreferences;
import org.opentripplanner.routing.api.request.request.filter.SelectRequest;
import org.opentripplanner.routing.api.request.request.filter.TransitFilterRequest;
import org.opentripplanner.transit.model.basic.MainAndSubMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class RouteRequestMapper {

  @Nonnull
  public static RouteRequest toRouteRequest(
    DataFetchingEnvironment environment,
    GraphQLRequestContext context
  ) {
    RouteRequest request = context.defaultRouteRequest();

    var args = new GraphQLTypes.GraphQLQueryTypePlanConnectionArgs(environment.getArguments());
    var dateTime = args.getGraphQLDateTime();
    if (dateTime.getGraphQLEarliestDeparture() != null) {
      request.setDateTime(args.getGraphQLDateTime().getGraphQLEarliestDeparture().toInstant());
    } else if (dateTime.getGraphQLLatestArrival() != null) {
      request.setDateTime(args.getGraphQLDateTime().getGraphQLLatestArrival().toInstant());
    } else {
      request.setDateTime(Instant.now());
    }
    request.setFrom(parseGenericLocation(args.getGraphQLOrigin()));
    request.setTo(parseGenericLocation(args.getGraphQLDestination()));
    request.setLocale(GraphQLUtils.getLocale(environment, args.getGraphQLLocale()));
    if (args.getGraphQLSearchWindow() != null) {
      request.setSearchWindow(args.getGraphQLSearchWindow());
    }

    if (args.getGraphQLBefore() != null) {
      request.setPageCursorFromEncoded(args.getGraphQLBefore());
      if (args.getGraphQLLast() != null) {
        request.setNumItineraries(args.getGraphQLLast());
      }
    } else if (args.getGraphQLAfter() != null) {
      request.setPageCursorFromEncoded(args.getGraphQLAfter());
      if (args.getGraphQLFirst() != null) {
        request.setNumItineraries(args.getGraphQLFirst());
      }
    } else if (args.getGraphQLNumberOfItineraries() != null) {
      request.setNumItineraries(args.getGraphQLNumberOfItineraries());
    }

    request.withPreferences(preferences -> setPreferences(preferences, args, environment));

    setModes(request, args.getGraphQLModes(), environment);

    return request;
  }

  private static void setPreferences(
    RoutingPreferences.Builder prefs,
    GraphQLTypes.GraphQLQueryTypePlanConnectionArgs args,
    DataFetchingEnvironment environment
  ) {
    prefs.withItineraryFilter(filters ->
      setItineraryFilters(filters, args.getGraphQLItineraryFilter())
    );
    prefs.withTransit(transit -> setTransitPreferences(transit, args, environment));
  }

  private static void setItineraryFilters(
    ItineraryFilterPreferences.Builder filterPreferences,
    GraphQLTypes.GraphQLPlanItineraryFilterInput filters
  ) {
    if (filters.getGraphQLItineraryFilterDebugProfile() != null) {
      filterPreferences.withDebug(
        ItineraryFilterDebugProfileMapper.map(filters.getGraphQLItineraryFilterDebugProfile())
      );
    }
    if (filters.getGraphQLGroupSimilarityKeepOne() != null) {
      filterPreferences.withGroupSimilarityKeepOne(filters.getGraphQLGroupSimilarityKeepOne());
    }
    if (filters.getGraphQLGroupSimilarityKeepThree() != null) {
      filterPreferences.withGroupSimilarityKeepThree(filters.getGraphQLGroupSimilarityKeepThree());
    }
    if (filters.getGraphQLGroupedOtherThanSameLegsMaxCostMultiplier() != null) {
      filterPreferences.withGroupedOtherThanSameLegsMaxCostMultiplier(
        filters.getGraphQLGroupedOtherThanSameLegsMaxCostMultiplier()
      );
    }
  }

  private static void setTransitPreferences(
    TransitPreferences.Builder preferences,
    GraphQLTypes.GraphQLQueryTypePlanConnectionArgs args,
    DataFetchingEnvironment environment
  ) {
    var modes = args.getGraphQLModes();
    var transit = getTransitModes(environment);
    if (!Boolean.TRUE.equals(modes.getGraphQLDirectOnly()) && transit.size() > 0) {
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
      preferences.setReluctanceForMode(reluctanceForMode);
    }
  }

  /**
   * TODO this doesn't support multiple street modes yet
   */
  private static void setModes(
    RouteRequest request,
    GraphQLTypes.GraphQLPlanModesInput modesInput,
    DataFetchingEnvironment environment
  ) {
    var direct = modesInput.getGraphQLDirect();
    if (Boolean.TRUE.equals(modesInput.getGraphQLTransitOnly())) {
      request.journey().direct().setMode(StreetMode.NOT_SET);
    } else if (direct != null && direct.size() > 0) {
      request.journey().direct().setMode(DirectModeMapper.map(direct.getFirst()));
    }

    var transit = modesInput.getGraphQLTransit();
    if (Boolean.TRUE.equals(modesInput.getGraphQLDirectOnly())) {
      request.journey().transit().disable();
    } else if (transit != null) {
      var access = transit.getGraphQLAccess();
      if (access != null && access.size() > 0) {
        request.journey().access().setMode(AccessModeMapper.map(access.getFirst()));
      }

      var egress = transit.getGraphQLEgress();
      if (egress != null && egress.size() > 0) {
        request.journey().egress().setMode(EgressModeMapper.map(egress.getFirst()));
      }

      var transfer = transit.getGraphQLTransfer();
      if (transfer != null && transfer.size() > 0) {
        request.journey().transfer().setMode(TransferModeMapper.map(transfer.getFirst()));
      }

      var transitModes = getTransitModes(environment);
      if (transitModes.size() > 0) {
        var filterRequestBuilder = TransitFilterRequest.of();
        var mainAndSubModes = transitModes
          .stream()
          .map(mode ->
            new MainAndSubMode(
              TransitModeMapper.map(
                GraphQLTypes.GraphQLTransitMode.valueOf((String) mode.get("mode"))
              )
            )
          )
          .toList();
        filterRequestBuilder.addSelect(
          SelectRequest.of().withTransportModes(mainAndSubModes).build()
        );
        request.journey().transit().setFilters(List.of(filterRequestBuilder.build()));
      }
    }
  }

  /**
   * This methods returns list of modes and their costs from the argument structure:
   * modes.transit.transit. This methods circumvents a bug in graphql-codegen as getting a list of
   * input objects is not possible through using the generated types in {@link GraphQLTypes}.
   * <p>
   * TODO this ugliness can be removed when the bug gets fixed
   */
  private static List<Map<String, Object>> getTransitModes(DataFetchingEnvironment environment) {
    if (environment.containsArgument("modes")) {
      Map<String, Object> modesArgs = environment.getArgument("modes");
      if (modesArgs.containsKey("transit")) {
        Map<String, Object> transitArgs = (Map<String, Object>) modesArgs.get("transit");
        if (transitArgs.containsKey("transit")) {
          return (List<Map<String, Object>>) transitArgs.get("transit");
        }
      }
    }
    return List.of();
  }

  private static GenericLocation parseGenericLocation(
    GraphQLTypes.GraphQLPlanLabeledLocationInput locationInput
  ) {
    var stopLocation = locationInput.getGraphQLLocation().getGraphQLStopLocation();
    if (stopLocation.getGraphQLStopLocationId() != null) {
      var stopId = stopLocation.getGraphQLStopLocationId();
      if (FeedScopedId.isValidString(stopId)) {
        return new GenericLocation(
          locationInput.getGraphQLLabel(),
          FeedScopedId.parse(stopId),
          null,
          null
        );
      } else {
        throw new IllegalArgumentException("Stop id %s is not of valid format.".formatted(stopId));
      }
    }

    var coordinate = locationInput.getGraphQLLocation().getGraphQLCoordinate();
    return new GenericLocation(
      locationInput.getGraphQLLabel(),
      null,
      coordinate.getGraphQLLatitude(),
      coordinate.getGraphQLLongitude()
    );
  }
}
