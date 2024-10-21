package org.opentripplanner.ext.sorlandsbanen;

import java.util.Collection;
import java.util.function.BiFunction;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.spi.ExtraMcRouterSearch;
import org.opentripplanner.raptor.spi.RaptorTransitDataProvider;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgresses;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RoutingAccessEgress;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.request.RaptorRoutingRequestTransitData;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.StopLocation;

/**
 * This is basically a big hack to produce results containing "Sørlandsbanen" in Norway. This
 * railroad line is slow and goes inland fare from where people live. Despite this, people and the
 * operator want to show it in the results for log travel along the southern part of Norway where
 * ii is an option. Tuning the search has proven to be challenging. It is solved here by doing
 * two searches. One normal search and one where the rail is given a big cost advantage over coach.
 * If train results are found in the second search, then it is added to the results of the first
 * search. Everything found in the first search is always returned.
 */
public class EnturSorlandsbanenService {

  private static final double SOUTH_BOARDER_LIMIT = 59.1;
  private static final int MIN_DISTANCE_LIMIT = 120_000;


  public ExtraMcRouterSearch<TripSchedule> createMcRouterFactory(RouteRequest request, AccessEgresses accessEgresses, TransitLayer transitLayer) {
    WgsCoordinate from = findStopCoordinate(
      request.from(),
      accessEgresses.getAccesses(),
      transitLayer
    );
    WgsCoordinate to = findStopCoordinate(request.to(), accessEgresses.getEgresses(), transitLayer);

    if (from.latitude() > SOUTH_BOARDER_LIMIT && to.latitude() > SOUTH_BOARDER_LIMIT) {
      return null;
    }

    double distance = from.distanceTo(to);
    if (distance < MIN_DISTANCE_LIMIT) {
      return null;
    }

    return new ExtraMcRouterSearch<>() {
      @Override
      public RaptorTransitDataProvider<TripSchedule> createTransitDataAlternativeSearch(RaptorTransitDataProvider<TripSchedule> transitDataMainSearch) {
        return new RaptorRoutingRequestTransitData(
          (RaptorRoutingRequestTransitData)transitDataMainSearch,
          new CoachCostCalculator<>(transitDataMainSearch.multiCriteriaCostCalculator())
        );
      }

      @Override
      public BiFunction<Collection<RaptorPath<TripSchedule>>, Collection<RaptorPath<TripSchedule>>, Collection<RaptorPath<TripSchedule>>> merger() {
        return new MergePaths<>();
      }
    };
  }

  /**
   * Find a coordinate matching the given location, in order:
   *  - First return the coordinate of the location if it exists.
   *  - Then loop through the access/egress stops and try to find the
   *    stop or station given by the location id, return the stop/station coordinate.
   *  - Return the fist stop in the access/egress list coordinate.
   */
  @SuppressWarnings("ConstantConditions")
  private static WgsCoordinate findStopCoordinate(
    GenericLocation location,
    Collection<? extends RoutingAccessEgress> accessEgress,
    TransitLayer transitLayer
  ) {
    if (location.lat != null) {
      return new WgsCoordinate(location.lat, location.lng);
    }

    StopLocation firstStop = null;
    for (RoutingAccessEgress it : accessEgress) {
      StopLocation stop = transitLayer.getStopByIndex(it.stop());
      if (stop.getId().equals(location.stopId)) {
        return stop.getCoordinate();
      }
      if (idIsParentStation(stop, location.stopId)) {
        return stop.getParentStation().getCoordinate();
      }
      if (firstStop == null) {
        firstStop = stop;
      }
    }
    return firstStop.getCoordinate();
  }

  private static boolean idIsParentStation(StopLocation stop, FeedScopedId pId) {
    return stop.getParentStation() != null && stop.getParentStation().getId().equals(pId);
  }
}
