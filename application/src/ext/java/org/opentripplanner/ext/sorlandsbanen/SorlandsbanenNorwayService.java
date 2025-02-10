package org.opentripplanner.ext.sorlandsbanen;

import java.util.Collection;
import java.util.function.BiFunction;
import javax.annotation.Nullable;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.spi.ExtraMcRouterSearch;
import org.opentripplanner.raptor.spi.RaptorTransitDataProvider;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgresses;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RaptorTransitData;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RoutingAccessEgress;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.request.RaptorRoutingRequestTransitData;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.StopLocation;

/**
 * This service is responsible for producing results with rail for the south of Norway. The rail
 * line is called "Sørlandsbanen". This rail line is slow and goes inland far from where people
 * live. Despite this, people and the operator want to show it in the results for log travel along
 * the southern part of Norway where it is an option. Tuning the search has proven to be
 * challenging. It is solved here by doing two searches. One normal search and one where the rail
 * is given a big cost advantage over coach. If train results are found in the second search, then
 * it is added to the results of the first search. Everything found in the first search is always
 * returned.
 */
public class SorlandsbanenNorwayService {

  private static final double SOUTH_BORDER_LIMIT = 59.1;
  private static final int MIN_DISTANCE_LIMIT = 120_000;

  @Nullable
  public ExtraMcRouterSearch<TripSchedule> createExtraMcRouterSearch(
    RouteRequest request,
    AccessEgresses accessEgresses,
    RaptorTransitData raptorTransitData
  ) {
    WgsCoordinate from = findStopCoordinate(
      request.from(),
      accessEgresses.getAccesses(),
      raptorTransitData
    );
    WgsCoordinate to = findStopCoordinate(
      request.to(),
      accessEgresses.getEgresses(),
      raptorTransitData
    );

    if (from.isNorthOf(SOUTH_BORDER_LIMIT) && to.isNorthOf(SOUTH_BORDER_LIMIT)) {
      return null;
    }

    double distance = from.distanceTo(to);
    if (distance < MIN_DISTANCE_LIMIT) {
      return null;
    }

    return new ExtraMcRouterSearch<>() {
      @Override
      public RaptorTransitDataProvider<TripSchedule> createTransitDataAlternativeSearch(
        RaptorTransitDataProvider<TripSchedule> transitDataMainSearch
      ) {
        return new RaptorRoutingRequestTransitData(
          (RaptorRoutingRequestTransitData) transitDataMainSearch,
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
   *  - Return the stop coordinate of the first access/egress in the list.
   */
  @SuppressWarnings("ConstantConditions")
  private static WgsCoordinate findStopCoordinate(
    GenericLocation location,
    Collection<? extends RoutingAccessEgress> accessEgress,
    RaptorTransitData raptorTransitData
  ) {
    if (location.lat != null) {
      return new WgsCoordinate(location.lat, location.lng);
    }

    StopLocation firstStop = null;
    for (RoutingAccessEgress it : accessEgress) {
      StopLocation stop = raptorTransitData.getStopByIndex(it.stop());
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
