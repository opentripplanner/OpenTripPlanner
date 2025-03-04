package org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opentripplanner.routing.algorithm.raptoradapter.api.DefaultTripPattern;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.GeneralizedCostParameters;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class GeneralizedCostParametersMapper {

  public static GeneralizedCostParameters map(
    RouteRequest request,
    List<? extends DefaultTripPattern> patternIndex
  ) {
    var builder = GeneralizedCostParameters.of();
    var preferences = request.preferences();

    builder
      .transferCost(preferences.transfer().cost())
      .waitReluctanceFactor(preferences.transfer().waitReluctance());

    StreetMode mode = request.journey().transfer().mode();
    if (mode == StreetMode.BIKE) {
      builder.boardCost(preferences.bike().boardCost());
    } else if (mode == StreetMode.CAR) {
      builder.boardCost(preferences.car().boardCost());
    } else {
      builder.boardCost(preferences.walk().boardCost());
    }

    builder.transitReluctanceFactors(
      mapTransitReluctance(preferences.transit().reluctanceForMode())
    );
    builder.wheelchairEnabled(request.wheelchair());
    builder.wheelchairAccessibility(preferences.wheelchair().trip());

    final Set<FeedScopedId> unpreferredRoutes = Set.copyOf(
      request.journey().transit().unpreferredRoutes()
    );
    final Set<FeedScopedId> unpreferredAgencies = Set.copyOf(
      request.journey().transit().unpreferredAgencies()
    );

    if (!unpreferredRoutes.isEmpty() || !unpreferredAgencies.isEmpty()) {
      final BitSet unpreferredPatterns = new BitSet();
      for (var pattern : patternIndex) {
        if (
          pattern != null &&
          (unpreferredRoutes.contains(pattern.route().getId()) ||
            unpreferredAgencies.contains(pattern.route().getAgency().getId()))
        ) {
          unpreferredPatterns.set(pattern.patternIndex());
        }
      }
      builder.unpreferredPatterns(unpreferredPatterns);
      builder.unpreferredCost(preferences.transit().unpreferredCost());
    }

    return builder.build();
  }

  public static double[] mapTransitReluctance(Map<TransitMode, Double> map) {
    if (map.isEmpty()) {
      return null;
    }

    // The transit reluctance is arranged in an array with the {@link TransitMode} ordinal
    // as an index. This make the lookup very fast and the size of the array small.
    // We could get away with a smaller array if we kept an index from mode to index
    // and passed that into the transit layer and used it to set the
    // {@link TripScheduleWithOffset#transitReluctanceIndex}, but this is difficult with the
    // current transit model design.
    double[] transitReluctance = new double[TransitMode.values().length];
    Arrays.fill(transitReluctance, GeneralizedCostParameters.DEFAULT_TRANSIT_RELUCTANCE);
    for (TransitMode mode : map.keySet()) {
      transitReluctance[mode.ordinal()] = map.get(mode);
    }
    return transitReluctance;
  }
}
