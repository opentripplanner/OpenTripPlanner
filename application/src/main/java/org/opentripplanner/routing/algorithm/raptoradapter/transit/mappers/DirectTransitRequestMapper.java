package org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.request.SearchParams;
import org.opentripplanner.raptor.direct.api.RaptorDirectTransitRequest;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.AccessEgressWithExtraCost;
import org.opentripplanner.routing.api.request.RouteRequest;

public class DirectTransitRequestMapper {

  /// Map the request into a request object for the direct transit search. Will return empty if
  /// the direct transit search shouldn't be run.
  public static Optional<RaptorDirectTransitRequest> map(
    RouteRequest request,
    SearchParams searchParamsUsed
  ) {
    var directTransitRequestOpt = request.preferences().transit().directTransit();
    if (directTransitRequestOpt.isEmpty()) {
      return Optional.empty();
    }
    var rel = directTransitRequestOpt.orElseThrow();
    Collection<? extends RaptorAccessEgress> access = searchParamsUsed.accessPaths();
    Collection<? extends RaptorAccessEgress> egress = searchParamsUsed.egressPaths();

    access = filterAccessEgressNoOpeningHours(access);
    egress = filterAccessEgressNoOpeningHours(egress);

    if (rel.maxAccessEgressDuration().isPresent()) {
      var maxDuration = rel.maxAccessEgressDuration().get();
      access = filterAccessEgressByDuration(access, maxDuration);
      egress = filterAccessEgressByDuration(egress, maxDuration);
    }
    if (rel.isExtraReluctanceAddedToAccessAndEgress()) {
      double f = rel.extraAccessEgressReluctance();
      access = decorateAccessEgressWithExtraCost(access, f);
      egress = decorateAccessEgressWithExtraCost(egress, f);
    }
    if (access.isEmpty() || egress.isEmpty()) {
      return Optional.empty();
    }
    var directRequest = RaptorDirectTransitRequest.of()
      .addAccessPaths(access)
      .addEgressPaths(egress)
      .searchWindowInSeconds(searchParamsUsed.searchWindowInSeconds())
      .earliestDepartureTime(searchParamsUsed.earliestDepartureTime())
      .withRelaxC1(RaptorRequestMapper.mapRelaxCost(rel.costRelaxFunction()))
      .build();
    return Optional.of(directRequest);
  }

  private static List<? extends RaptorAccessEgress> filterAccessEgressByDuration(
    Collection<? extends RaptorAccessEgress> list,
    Duration maxDuration
  ) {
    return list
      .stream()
      .filter(ae -> ae.durationInSeconds() <= maxDuration.toSeconds())
      .toList();
  }

  private static List<? extends RaptorAccessEgress> filterAccessEgressNoOpeningHours(
    Collection<? extends RaptorAccessEgress> list
  ) {
    return list
      .stream()
      .filter(it -> !it.hasOpeningHours())
      .toList();
  }

  private static List<? extends RaptorAccessEgress> decorateAccessEgressWithExtraCost(
    Collection<? extends RaptorAccessEgress> list,
    double costFactor
  ) {
    return list
      .stream()
      .map(it -> new AccessEgressWithExtraCost(it, costFactor))
      .toList();
  }
}
