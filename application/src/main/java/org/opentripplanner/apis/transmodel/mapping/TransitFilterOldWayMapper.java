package org.opentripplanner.apis.transmodel.mapping;

import graphql.schema.DataFetchingEnvironment;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.opentripplanner.api.model.transit.FeedScopedIdMapper;
import org.opentripplanner.apis.transmodel.model.TransmodelTransportSubmode;
import org.opentripplanner.apis.transmodel.support.DataFetcherDecorator;
import org.opentripplanner.apis.transmodel.support.GqlUtil;
import org.opentripplanner.routing.api.request.request.TransitRequestBuilder;
import org.opentripplanner.routing.api.request.request.filter.SelectRequest;
import org.opentripplanner.transit.model.basic.MainAndSubMode;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class TransitFilterOldWayMapper {

  private final FeedScopedIdMapper idMapper;

  TransitFilterOldWayMapper(FeedScopedIdMapper idMapper) {
    this.idMapper = idMapper;
  }

  @SuppressWarnings("unchecked")
  void mapFilter(
    DataFetchingEnvironment environment,
    DataFetcherDecorator callWith,
    TransitRequestBuilder transitBuilder
  ) {
    if (
      !(GqlUtil.hasArgument(environment, "modes") &&
        ((Map<String, Object>) environment.getArgument("modes")).containsKey("transportModes")) &&
      !GqlUtil.hasArgument(environment, "whiteListed") &&
      !GqlUtil.hasArgument(environment, "banned")
    ) {
      return;
    }
    var selectorBuilders = new ArrayList<SelectRequest.Builder>();

    var whiteListedAgencies = new ArrayList<FeedScopedId>();
    callWith.argument("whiteListed.authorities", (Collection<String> authorities) ->
      whiteListedAgencies.addAll(idMapper.parseListNullSafe(authorities))
    );
    if (!whiteListedAgencies.isEmpty()) {
      selectorBuilders.add(SelectRequest.of().withAgencies(whiteListedAgencies));
    }

    var whiteListedLines = new ArrayList<FeedScopedId>();
    callWith.argument("whiteListed.lines", (List<String> lines) ->
      whiteListedLines.addAll(idMapper.parseListNullSafe(lines))
    );
    if (!whiteListedLines.isEmpty()) {
      selectorBuilders.add(SelectRequest.of().withRoutes(whiteListedLines));
    }

    // Create modes filter for the request
    final var tModes = mapTransitModes(environment);
    if (tModes.isEmpty()) {
      transitBuilder.disable();
      return;
    }

    var selectors = buildFiltersWithModes(selectorBuilders, tModes);

    transitBuilder.withFilter(filterBuilder -> {
      for (var s : selectors) {
        filterBuilder.addSelect(s);
      }

      var bannedAgencies = new ArrayList<FeedScopedId>();
      callWith.argument("banned.authorities", (Collection<String> authorities) ->
        bannedAgencies.addAll(idMapper.parseListNullSafe(authorities))
      );
      if (!bannedAgencies.isEmpty()) {
        filterBuilder.addNot(SelectRequest.of().withAgencies(bannedAgencies).build());
      }

      var bannedLines = new ArrayList<FeedScopedId>();
      callWith.argument("banned.lines", (List<String> lines) ->
        bannedLines.addAll(idMapper.parseListNullSafe(lines))
      );
      if (!bannedLines.isEmpty()) {
        filterBuilder.addNot(SelectRequest.of().withRoutes(bannedLines).build());
      }
    });
  }

  /**
   * Return transit modes. If this method returns empty the transit search should be disabled.
   * This happens is the transit "modes" is defined and empty. If not defined the default is
   * ALL transit modes.
   */
  private static List<MainAndSubMode> mapTransitModes(DataFetchingEnvironment environment) {
    final List<MainAndSubMode> tModes = new ArrayList<>();
    if (!GqlUtil.hasArgument(environment, "modes")) {
      return MainAndSubMode.all();
    }
    Map<String, Object> modesInput = environment.getArgument("modes");
    if (modesInput.get("transportModes") == null) {
      return MainAndSubMode.all();
    }

    List<Map<String, ?>> transportModes = (List<Map<String, ?>>) modesInput.get("transportModes");

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
    return tModes;
  }

  /**
   * Finish building the selectors. Add modes to all existing selectors, or create a new
   * selector using the provided modes.
   */
  private static List<SelectRequest> buildFiltersWithModes(
    ArrayList<SelectRequest.Builder> selectors,
    List<MainAndSubMode> tModes
  ) {
    if (selectors.isEmpty()) {
      return List.of(SelectRequest.of().withTransportModes(tModes).build());
    }
    return selectors
      .stream()
      .peek(it -> it.withTransportModes(tModes))
      .map(it -> it.build())
      .toList();
  }
}
