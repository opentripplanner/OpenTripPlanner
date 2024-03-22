package org.opentripplanner.apis.gtfs.mapping.routerequest;

import static org.opentripplanner.apis.gtfs.mapping.routerequest.ArgumentUtils.getTransitModes;

import graphql.schema.DataFetchingEnvironment;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.apis.gtfs.mapping.TransitModeMapper;
import org.opentripplanner.framework.collection.CollectionUtils;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.request.JourneyRequest;
import org.opentripplanner.routing.api.request.request.filter.SelectRequest;
import org.opentripplanner.routing.api.request.request.filter.TransitFilterRequest;
import org.opentripplanner.transit.model.basic.MainAndSubMode;

public class ModePreferencesMapper {

  /**
   * TODO this doesn't support multiple street modes yet
   */
  static void setModes(
    JourneyRequest journey,
    GraphQLTypes.GraphQLPlanModesInput modesInput,
    DataFetchingEnvironment environment
  ) {
    var direct = modesInput.getGraphQLDirect();
    if (Boolean.TRUE.equals(modesInput.getGraphQLTransitOnly())) {
      journey.direct().setMode(StreetMode.NOT_SET);
    } else if (!CollectionUtils.isEmpty(direct)) {
      journey.direct().setMode(DirectModeMapper.map(direct.getFirst()));
    }

    var transit = modesInput.getGraphQLTransit();
    if (Boolean.TRUE.equals(modesInput.getGraphQLDirectOnly())) {
      journey.transit().disable();
    } else if (transit != null) {
      var access = transit.getGraphQLAccess();
      if (!CollectionUtils.isEmpty(access)) {
        journey.access().setMode(AccessModeMapper.map(access.getFirst()));
      }

      var egress = transit.getGraphQLEgress();
      if (!CollectionUtils.isEmpty(egress)) {
        journey.egress().setMode(EgressModeMapper.map(egress.getFirst()));
      }

      var transfer = transit.getGraphQLTransfer();
      if (!CollectionUtils.isEmpty(transfer)) {
        journey.transfer().setMode(TransferModeMapper.map(transfer.getFirst()));
      }
      validateStreetModes(journey);

      var transitModes = getTransitModes(environment);
      if (!CollectionUtils.isEmpty(transitModes)) {
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
        journey.transit().setFilters(List.of(filterRequestBuilder.build()));
      }
    }
  }

  /**
   * TODO this doesn't support multiple street modes yet
   */
  private static void validateStreetModes(JourneyRequest journey) {
    Set<StreetMode> modes = new HashSet();
    modes.add(journey.access().mode());
    modes.add(journey.egress().mode());
    modes.add(journey.transfer().mode());
    if (modes.contains(StreetMode.BIKE) && modes.size() != 1) {
      throw new IllegalArgumentException(
        "If BICYCLE is used for access, egress or transfer, then it should be used for all."
      );
    }
  }
}
