package org.opentripplanner.apis.gtfs.mapping.routerequest;

import static org.opentripplanner.apis.gtfs.mapping.routerequest.ArgumentUtils.getPlanTransitModes;
import static org.opentripplanner.apis.gtfs.mapping.routerequest.StreetModeMapper.getStreetModeForRouting;
import static org.opentripplanner.apis.gtfs.mapping.routerequest.StreetModeMapper.validateStreetModes;

import graphql.schema.DataFetchingEnvironment;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.apis.gtfs.mapping.TransitModeMapper;
import org.opentripplanner.routing.api.request.request.JourneyRequestBuilder;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.api.request.request.TransitRequestBuilder;
import org.opentripplanner.routing.api.request.request.filter.SelectRequest;
import org.opentripplanner.routing.api.request.request.filter.TransitFilterRequest;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.transit.model.basic.MainAndSubMode;
import org.opentripplanner.transit.model.basic.NarrowedTransitMode;
import org.opentripplanner.utils.collection.CollectionUtils;
import org.opentripplanner.utils.time.DurationUtils;

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
      var rentalDuration = getRentalDurationByQueryArgs(args);
      journey.withDirect(new StreetRequest(getStreetModeForRouting(streetModes), rentalDuration));
    }

    var transit = modesInput.getGraphQLTransit();
    if (Boolean.TRUE.equals(modesInput.getGraphQLDirectOnly())) {
      journey.withTransit(TransitRequestBuilder::disable);
    } else if (transit == null) {
      // even if there are no transit modes set, we need to set the filter to get the route/agency
      // filters for flex
      setTransitFilters(journey, NarrowedTransitMode.all(), args);
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

      var planTransitModes = getPlanTransitModes(environment);
      if (planTransitModes == null) {
        // even when there are no transit modes set we need to set the filters because of the route/agency
        // includes/excludes
        setTransitFilters(journey, NarrowedTransitMode.all(), args);
      } else {
        if (planTransitModes.isEmpty()) {
          throw new IllegalArgumentException("Transit modes must not be empty.");
        }
        var narrowedModes = planTransitModes.stream().map(ModePreferencesMapper::map).toList();
        setTransitFilters(journey, narrowedModes, args);
      }
    }
  }

  private static NarrowedTransitMode map(GraphQLTypes.GraphQLPlanTransitModePreferenceInput input) {
    if (input != null) {
      return new NarrowedTransitMode(
        TransitModeMapper.map(input.getGraphQLMode()),
        Collections.emptyList(),
        input.getGraphQLReplacement(),
        input.getGraphQLAllowedExtendedType(),
        input.getGraphQLForbiddenExtendedType()
      );
    } else {
      return null;
    }
  }

  /**
   * It may be a little surprising that the transit filters are mapped here. This
   * is because the mapping function needs to know the modes to build the correct
   * select request as it needs to be the first one in each transit filter request.
   */
  private static void setTransitFilters(
    JourneyRequestBuilder journey,
    List<NarrowedTransitMode> modes,
    GraphQLTypes.GraphQLQueryTypePlanConnectionArgs args
  ) {
    var graphQlFilters = Optional.ofNullable(args.getGraphQLPreferences())
      .map(GraphQLTypes.GraphQLPlanPreferencesInput::getGraphQLTransit)
      .map(GraphQLTypes.GraphQLTransitPreferencesInput::getGraphQLFilters)
      .orElse(List.of());
    if (CollectionUtils.hasValue(graphQlFilters)) {
      var mainModes = modes
        .stream()
        .map(mode -> new MainAndSubMode(mode.getMode()))
        .toList();
      var filters = FilterMapper.mapFilters(mainModes, graphQlFilters);
      journey.withTransit(b -> b.withFilters(filters));
    }
    // if there isn't a transit filter or a mode set, then we can keep the default which is to include
    // everything
    else if (!modes.equals(NarrowedTransitMode.all())) {
      var filter = TransitFilterRequest.of()
        .addSelect(SelectRequest.of().withNarrowedTransportModes(modes).build())
        .build();
      journey.withTransit(b -> b.withFilters(List.of(filter)));
    }
  }

  /**
   * return car rental duration, if it was set in graphql query.
   * This method exist, to handle all the null checks.
   */
  @Nullable
  private static Duration getRentalDurationByQueryArgs(
    GraphQLTypes.GraphQLQueryTypePlanConnectionArgs args
  ) {
    var graphQLStreet = args.getGraphQLPreferences().getGraphQLStreet();
    if (graphQLStreet != null) {
      var graphQLCar = graphQLStreet.getGraphQLCar();
      if (graphQLCar != null) {
        var graphQLRental = graphQLCar.getGraphQLRental();
        if (graphQLRental != null) {
          var rentalDuration = graphQLRental.getGraphQLRentalDuration();
          if (rentalDuration != null) {
            return DurationUtils.requireNonNegative(rentalDuration, "rentalDuration");
          }
        }
      }
    }
    return null;
  }
}
