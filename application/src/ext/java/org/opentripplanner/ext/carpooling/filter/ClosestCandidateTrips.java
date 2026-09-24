package org.opentripplanner.ext.carpooling.filter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.ext.carpooling.routing.CarpoolTripWithVertices;
import org.opentripplanner.street.geometry.SphericalDistanceLibrary;
import org.opentripplanner.street.geometry.WgsCoordinate;

/**
 * Bounds the number of candidate trips a request evaluates: when more trips pass the pre-filters
 * than the configured maximum, only the ones whose route passes closest to the passenger are kept.
 * <p>
 * Every candidate trip costs a fixed amount of work per request, so without a bound the request
 * time grows with the feed. The distance from the passenger to the route is a cheap proxy for how
 * promising a trip is: the farther the driver has to leave the route, the more of the deviation
 * budget the detour eats.
 */
public final class ClosestCandidateTrips {

  private ClosestCandidateTrips() {}

  /**
   * The {@code max} trips whose routes pass closest to the passenger, closest first. Closeness is
   * the sum over the passenger points of the beeline distance from the point to the nearest route
   * segment; an access or egress request has one point, a direct request two. Ties, and the order
   * when nothing is cut, follow the trip id: the evaluation order decides later ties (one
   * candidate per stop and slot), and it must not depend on the repository's iteration order.
   */
  public static List<CarpoolTripWithVertices> closest(
    Collection<CarpoolTripWithVertices> trips,
    List<WgsCoordinate> passengerPoints,
    int max
  ) {
    if (max < 1) {
      throw new IllegalArgumentException("max must be positive");
    }
    var sorted = new ArrayList<>(trips);
    sorted.sort(Comparator.comparing(trip -> trip.trip().getId()));
    if (sorted.size() <= max) {
      return sorted;
    }
    var scored = new ArrayList<Scored>(sorted.size());
    for (var trip : sorted) {
      double score = 0;
      for (var point : passengerPoints) {
        score += distanceToRoute(trip.trip(), point);
      }
      scored.add(new Scored(trip, score));
    }
    scored.sort(Comparator.comparingDouble(Scored::metres));
    return scored.subList(0, max).stream().map(Scored::trip).toList();
  }

  /** Beeline distance in metres from {@code point} to the nearest segment of the trip's route. */
  static double distanceToRoute(CarpoolTrip trip, WgsCoordinate point) {
    List<WgsCoordinate> route = trip.routePoints();
    Coordinate p = point.asJtsCoordinate();
    if (route.size() == 1) {
      return SphericalDistanceLibrary.fastDistance(p, route.getFirst().asJtsCoordinate());
    }
    double best = Double.POSITIVE_INFINITY;
    for (int i = 0; i < route.size() - 1; i++) {
      double d = SphericalDistanceLibrary.fastDistance(
        p,
        route.get(i).asJtsCoordinate(),
        route.get(i + 1).asJtsCoordinate()
      );
      best = Math.min(best, d);
    }
    return best;
  }

  private record Scored(CarpoolTripWithVertices trip, double metres) {}
}
