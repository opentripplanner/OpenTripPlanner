package org.opentripplanner.place.nearbystopfinder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.place.NearbyStopFinder;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.preference.WalkPreferences;
import org.opentripplanner.street.geometry.SphericalDistanceLibrary;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.transit.model.site.RegularStop;

public class StraightLineNearbyStopFinder implements NearbyStopFinder {

  private final Duration durationLimit;
  private final Function<Envelope, Collection<RegularStop>> queryNearbyStops;

  public StraightLineNearbyStopFinder(
    Function<Envelope, Collection<RegularStop>> queryNearbyStops
  ) {
    this(queryNearbyStops, Duration.ofHours(10000));
  }

  public StraightLineNearbyStopFinder(
    Function<Envelope, Collection<RegularStop>> queryNearbyStops,
    Duration durationLimit
  ) {
    this.queryNearbyStops = queryNearbyStops;
    // TODO move request specific parameters to method
    this.durationLimit = durationLimit;
  }

  /**
   * Return all stops within a certain radius of the given vertex, using straight-line distance
   * independent of streets. If the origin vertex is a StopVertex, the result will include it.
   */
  @Override
  public List<NearbyStop> findNearbyStops(Coordinate coordinate, double radiusMeters) {
    List<NearbyStop> stopsFound = new ArrayList<>();
    Envelope envelope = new Envelope(coordinate);
    envelope.expandBy(
      SphericalDistanceLibrary.metersToLonDegrees(radiusMeters, coordinate.y),
      SphericalDistanceLibrary.metersToDegrees(radiusMeters)
    );
    for (RegularStop it : queryNearbyStops.apply(envelope)) {
      double distance = Math.round(
        SphericalDistanceLibrary.distance(coordinate, it.getCoordinate().asJtsCoordinate())
      );
      if (distance < radiusMeters) {
        NearbyStop sd = new NearbyStop(it.getId(), distance, null, null);
        stopsFound.add(sd);
      }
    }

    stopsFound.sort(NearbyStop::compareTo);

    return stopsFound;
  }

  /**
   * Find nearby stops using straight line distance.
   */
  @Override
  public List<NearbyStop> findNearbyStops(
    Vertex vertex,
    RouteRequest routingRequest,
    StreetMode streetMode,
    boolean reverseDirection
  ) {
    return findNearbyStopsViaDirectTransfers(vertex);
  }

  private List<NearbyStop> findNearbyStopsViaDirectTransfers(Vertex vertex) {
    // TODO why we use default speed here?
    double limitMeters = durationLimit.toSeconds() * WalkPreferences.DEFAULT.speed();
    Coordinate c0 = vertex.getCoordinate();
    return findNearbyStops(c0, limitMeters);
  }
}
