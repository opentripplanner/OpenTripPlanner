package org.opentripplanner.apis.gtfs.mapping.routerequest;

import static org.opentripplanner.apis.gtfs.mapping.routerequest.ArgumentUtils.getTransitModes;

import graphql.schema.DataFetchingEnvironment;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.apis.gtfs.mapping.TransitModeMapper;
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
    } else if (direct != null) {
      if (direct.isEmpty()) {
        throw new IllegalArgumentException("Direct modes must not be empty.");
      }
      var streetModes = direct.stream().map(DirectModeMapper::map).toList();
      journey.direct().setMode(getStreetMode(streetModes));
    }

    var transit = modesInput.getGraphQLTransit();
    if (Boolean.TRUE.equals(modesInput.getGraphQLDirectOnly())) {
      journey.transit().disable();
    } else if (transit != null) {
      var access = transit.getGraphQLAccess();
      if (access != null) {
        if (access.isEmpty()) {
          throw new IllegalArgumentException("Access modes must not be empty.");
        }
        var streetModes = access.stream().map(AccessModeMapper::map).toList();
        journey.access().setMode(getStreetMode(streetModes));
      }

      var egress = transit.getGraphQLEgress();
      if (egress != null) {
        if (egress.isEmpty()) {
          throw new IllegalArgumentException("Egress modes must not be empty.");
        }
        var streetModes = egress.stream().map(EgressModeMapper::map).toList();
        journey.egress().setMode(getStreetMode(streetModes));
      }

      var transfer = transit.getGraphQLTransfer();
      if (transfer != null) {
        if (transfer.isEmpty()) {
          throw new IllegalArgumentException("Transfer modes must not be empty.");
        }
        var streetModes = transfer.stream().map(TransferModeMapper::map).toList();
        journey.transfer().setMode(getStreetMode(streetModes));
      }
      validateStreetModes(journey);

      var transitModes = getTransitModes(environment);
      if (transitModes != null) {
        if (transitModes.isEmpty()) {
          throw new IllegalArgumentException("Transit modes must not be empty.");
        }
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
   * Current support:
   * 1. If only one mode is defined, it needs to be WALK, BICYCLE, CAR or some parking mode.
   * 2. If two modes are defined, they can't be BICYCLE or CAR, and WALK needs to be one of them.
   * 3. More than two modes can't be defined for the same leg.
   * <p>
   * TODO future support:
   * 1. Any mode can be defined alone. If it's not used in a leg, the leg gets filtered away.
   * 2. If two modes are defined, they can't be BICYCLE or CAR. Usually WALK is required as the second
   *    mode but in some cases it's possible to define other modes as well such as BICYCLE_RENTAL together
   *    with SCOOTER_RENTAL. In that case, legs which don't use BICYCLE_RENTAL or SCOOTER_RENTAL would be filtered
   *    out.
   * 3. When more than two modes are used, some combinations are supported such as WALK, BICYCLE_RENTAL and SCOOTER_RENTAL.
   */
  private static StreetMode getStreetMode(List<StreetMode> modes) {
    if (modes.size() > 2) {
      throw new IllegalArgumentException(
        "Only one or two modes can be specified for a leg, got: %.".formatted(modes)
      );
    }
    if (modes.size() == 1) {
      var mode = modes.getFirst();
      // TODO in the future, we will support defining other modes alone as well and filter out legs
      // which don't contain the only specified mode as opposed to also returning legs which contain
      // only walking.
      if (!isAlwaysPresentInLeg(mode)) {
        throw new IllegalArgumentException(
          "For the time being, %s needs to be combined with WALK mode for the same leg.".formatted(
              mode
            )
        );
      }
      return mode;
    }
    if (modes.contains(StreetMode.BIKE)) {
      throw new IllegalArgumentException(
        "Bicycle can't be combined with other modes for the same leg: %s.".formatted(modes)
      );
    }
    if (modes.contains(StreetMode.CAR)) {
      throw new IllegalArgumentException(
        "Car can't be combined with other modes for the same leg: %s.".formatted(modes)
      );
    }
    if (!modes.contains(StreetMode.WALK)) {
      throw new IllegalArgumentException(
        "For the time being, WALK needs to be added as a mode for a leg when using %s and these two can't be used in the same leg.".formatted(
            modes
          )
      );
    }
    // Walk is currently always used as an implied mode when mode is not car.
    return modes.stream().filter(mode -> mode != StreetMode.WALK).findFirst().get();
  }

  private static boolean isAlwaysPresentInLeg(StreetMode mode) {
    return (
      mode == StreetMode.BIKE ||
      mode == StreetMode.CAR ||
      mode == StreetMode.WALK ||
      mode.includesParking()
    );
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
    if (modes.contains(StreetMode.CAR) && modes.size() != 1) {
      throw new IllegalArgumentException(
        "If CAR is used for access, egress or transfer, then it should be used for all."
      );
    }
  }
}
