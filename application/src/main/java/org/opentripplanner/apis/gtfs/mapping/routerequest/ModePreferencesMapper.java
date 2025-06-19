package org.opentripplanner.apis.gtfs.mapping.routerequest;

import static org.opentripplanner.apis.gtfs.mapping.routerequest.ArgumentUtils.getTransitModes;
import static org.opentripplanner.apis.gtfs.mapping.routerequest.StreetModeMapper.getStreetModeForRouting;
import static org.opentripplanner.apis.gtfs.mapping.routerequest.StreetModeMapper.validateStreetModes;

import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import java.util.Optional;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.apis.gtfs.mapping.TransitModeMapper;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.request.JourneyRequestBuilder;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.api.request.request.TransitRequestBuilder;
import org.opentripplanner.routing.api.request.request.filter.SelectRequest;
import org.opentripplanner.routing.api.request.request.filter.TransitFilterRequest;
import org.opentripplanner.transit.model.basic.MainAndSubMode;
import org.opentripplanner.utils.collection.CollectionUtils;

public class ModePreferencesMapper {

  /**
   * TODO this doesn't support multiple street modes yet
   */
  static void setModes(
    JourneyRequestBuilder journey,
    GraphQLTypes.GraphQLQueryTypePlanConnectionArgs args,
    DataFetchingEnvironment environment
  ) {
    var modesInput = args.getGraphQLModes();
    var direct = modesInput.getGraphQLDirect();
    if (Boolean.TRUE.equals(modesInput.getGraphQLTransitOnly())) {
      journey.withDirect(new StreetRequest(StreetMode.NOT_SET));
    } else if (direct != null) {
      if (direct.isEmpty()) {
        throw new IllegalArgumentException("Direct modes must not be empty.");
      }
      var streetModes = direct.stream().map(DirectModeMapper::map).toList();
      journey.withDirect(new StreetRequest(getStreetModeForRouting(streetModes)));
    }

    var transit = modesInput.getGraphQLTransit();
    if (Boolean.TRUE.equals(modesInput.getGraphQLDirectOnly())) {
      journey.withTransit(TransitRequestBuilder::disable);
    } else if (transit == null) {
      // even if there are no transit modes set, we need to set the filter to get the route/agency
      // filters for flex
      setTransitFilters(journey, MainAndSubMode.all(), args);
    } else {
      var access = transit.getGraphQLAccess();
      if (access != null) {
        if (access.isEmpty()) {
          throw new IllegalArgumentException("Access modes must not be empty.");
        }
        var streetModes = access.stream().map(AccessModeMapper::map).toList();
        journey.withAccess(new StreetRequest(getStreetModeForRouting(streetModes)));
      }

      var egress = transit.getGraphQLEgress();
      if (egress != null) {
        if (egress.isEmpty()) {
          throw new IllegalArgumentException("Egress modes must not be empty.");
        }
        var streetModes = egress.stream().map(EgressModeMapper::map).toList();
        journey.withEgress(new StreetRequest(getStreetModeForRouting(streetModes)));
      }

      var transfer = transit.getGraphQLTransfer();
      if (transfer != null) {
        if (transfer.isEmpty()) {
          throw new IllegalArgumentException("Transfer modes must not be empty.");
        }
        var streetModes = transfer.stream().map(TransferModeMapper::map).toList();
        journey.withTransfer(new StreetRequest(getStreetModeForRouting(streetModes)));
      }

      // TODO: This validation should be moved into the journey constructor (Feature Envy)
      validateStreetModes(journey.build());

      var transitModes = getTransitModes(environment);
      if (transitModes == null) {
        // even when there are no transit modes set we need to set the filters because of the route/agency
        // includes/excludes
        setTransitFilters(journey, MainAndSubMode.all(), args);
      } else {
        if (transitModes.isEmpty()) {
          throw new IllegalArgumentException("Transit modes must not be empty.");
        }
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
        setTransitFilters(journey, mainAndSubModes, args);
      }
    }
  }

  /**
   * It may be a little surprising that the transit filters are mapped here. This
   * is because the mapping function needs to know the modes to build the correct
   * select request as it needs to be the first one in each transit filter request.
   */
  private static void setTransitFilters(
    JourneyRequestBuilder journey,
    List<MainAndSubMode> modes,
    GraphQLTypes.GraphQLQueryTypePlanConnectionArgs args
  ) {
    var graphQlFilters = Optional.ofNullable(args.getGraphQLPreferences())
      .map(GraphQLTypes.GraphQLPlanPreferencesInput::getGraphQLTransit)
      .map(GraphQLTypes.GraphQLTransitPreferencesInput::getGraphQLFilters)
      .orElse(List.of());
    if (CollectionUtils.hasValue(graphQlFilters)) {
      var filters = FilterMapper.mapFilters(modes, graphQlFilters);
      journey.withTransit(b -> b.setFilters(filters));
    }
    // if there isn't a transit filter or a mode set, then we can keep the default which is to include
    // everything
    else if (!modes.equals(MainAndSubMode.all())) {
      var filter = TransitFilterRequest.of()
        .addSelect(SelectRequest.of().withTransportModes(modes).build())
        .build();
      journey.withTransit(b -> b.setFilters(List.of(filter)));
    }
  }
}
