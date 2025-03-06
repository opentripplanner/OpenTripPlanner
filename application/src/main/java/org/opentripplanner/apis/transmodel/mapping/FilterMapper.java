package org.opentripplanner.apis.transmodel.mapping;

import static org.opentripplanner.apis.transmodel.mapping.TransitIdMapper.mapIDsToDomainNullSafe;

import graphql.schema.DataFetchingEnvironment;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.opentripplanner.apis.transmodel.model.TransmodelTransportSubmode;
import org.opentripplanner.apis.transmodel.support.DataFetcherDecorator;
import org.opentripplanner.apis.transmodel.support.GqlUtil;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.filter.SelectRequest;
import org.opentripplanner.routing.api.request.request.filter.TransitFilter;
import org.opentripplanner.routing.api.request.request.filter.TransitFilterRequest;
import org.opentripplanner.transit.model.basic.MainAndSubMode;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class FilterMapper {

  @SuppressWarnings("unchecked")
  static void mapFilterOldWay(
    DataFetchingEnvironment environment,
    DataFetcherDecorator callWith,
    RouteRequest request
  ) {
    if (
      !(GqlUtil.hasArgument(environment, "modes") &&
        ((Map<String, Object>) environment.getArgument("modes")).containsKey("transportModes")) &&
      !GqlUtil.hasArgument(environment, "whiteListed") &&
      !GqlUtil.hasArgument(environment, "banned")
    ) {
      return;
    }

    var filterRequestBuilder = TransitFilterRequest.of();

    var bannedAgencies = new ArrayList<FeedScopedId>();
    callWith.argument("banned.authorities", (Collection<String> authorities) ->
      bannedAgencies.addAll(mapIDsToDomainNullSafe(authorities))
    );
    if (!bannedAgencies.isEmpty()) {
      filterRequestBuilder.addNot(SelectRequest.of().withAgencies(bannedAgencies).build());
    }

    var bannedLines = new ArrayList<FeedScopedId>();
    callWith.argument("banned.lines", (List<String> lines) ->
      bannedLines.addAll(mapIDsToDomainNullSafe(lines))
    );
    if (!bannedLines.isEmpty()) {
      filterRequestBuilder.addNot(SelectRequest.of().withRoutes(bannedLines).build());
    }

    var selectors = new ArrayList<SelectRequest.Builder>();

    var whiteListedAgencies = new ArrayList<FeedScopedId>();
    callWith.argument("whiteListed.authorities", (Collection<String> authorities) ->
      whiteListedAgencies.addAll(mapIDsToDomainNullSafe(authorities))
    );
    if (!whiteListedAgencies.isEmpty()) {
      selectors.add(SelectRequest.of().withAgencies(whiteListedAgencies));
    }

    var whiteListedLines = new ArrayList<FeedScopedId>();
    callWith.argument("whiteListed.lines", (List<String> lines) ->
      whiteListedLines.addAll(mapIDsToDomainNullSafe(lines))
    );
    if (!whiteListedLines.isEmpty()) {
      selectors.add(SelectRequest.of().withRoutes(whiteListedLines));
    }

    // Create modes filter for the request
    List<MainAndSubMode> tModes = new ArrayList<>();
    if (GqlUtil.hasArgument(environment, "modes")) {
      Map<String, Object> modesInput = environment.getArgument("modes");
      if (modesInput.get("transportModes") != null) {
        List<Map<String, ?>> transportModes = (List<Map<String, ?>>) modesInput.get(
          "transportModes"
        );
        // Disable transit if transit modes is defined and empty
        if (transportModes.isEmpty()) {
          request.journey().transit().disable();
          return;
        }

        for (Map<String, ?> modeWithSubmodes : transportModes) {
          if (modeWithSubmodes.containsKey("transportMode")) {
            var mainMode = (TransitMode) modeWithSubmodes.get("transportMode");

            if (modeWithSubmodes.containsKey("transportSubModes")) {
              var transportSubModes = (List<TransmodelTransportSubmode>) modeWithSubmodes.get(
                "transportSubModes"
              );
              for (TransmodelTransportSubmode submode : transportSubModes) {
                tModes.add(new MainAndSubMode(mainMode, SubMode.of(submode.getValue())));
              }
            } else {
              tModes.add(new MainAndSubMode(mainMode));
            }
          }
        }
      } else {
        tModes = MainAndSubMode.all();
      }
    } else {
      tModes = MainAndSubMode.all();
    }

    // Add modes filter to all existing selectors
    // If no selectors specified, create new one
    if (!selectors.isEmpty()) {
      for (var selector : selectors) {
        filterRequestBuilder.addSelect(selector.withTransportModes(tModes).build());
      }
    } else {
      filterRequestBuilder.addSelect(SelectRequest.of().withTransportModes(tModes).build());
    }

    request.journey().transit().setFilters(List.of(filterRequestBuilder.build()));
  }

  @SuppressWarnings("unchecked")
  static List<TransitFilter> mapFilterNewWay(List<Map<String, ?>> filters) {
    var filterRequests = new ArrayList<TransitFilter>();

    for (var filterInput : filters) {
      var filterRequestBuilder = TransitFilterRequest.of();

      if (filterInput.containsKey("select")) {
        for (var selectInput : (List<Map<String, List<?>>>) filterInput.get("select")) {
          filterRequestBuilder.addSelect(SelectRequestMapper.mapSelectRequest(selectInput));
        }
      }

      if (filterInput.containsKey("not")) {
        for (var selectInput : (List<Map<String, List<?>>>) filterInput.get("not")) {
          filterRequestBuilder.addNot(SelectRequestMapper.mapSelectRequest(selectInput));
        }
      }

      filterRequests.add(filterRequestBuilder.build());
    }

    return filterRequests;
  }
}
