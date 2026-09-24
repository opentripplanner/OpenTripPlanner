package org.opentripplanner.ext.carpooling.routing;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.astar.model.ShortestPathTree;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.ext.carpooling.util.CarReachableVertexSnapper.SnapResult;
import org.opentripplanner.street.geometry.SphericalDistanceLibrary;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Computes a trip's {@link CarpoolCorridor} from the static street graph: the routed baseline of
 * every leg, and per leg the transit stops inside its feasibility ellipse with the driving times
 * to serve them. Per leg this builds two ellipse-bounded car trees and reads the
 * times of every stop inside the ellipse's bounding box off them. Done once when the trip
 * arrives, on the updater's resolution thread, instead of once per request.
 */
public class CorridorBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(CorridorBuilder.class);

  /** Margin on the ellipse's bounding box, against the approximations in beeline and degree conversion. */
  private static final double ENVELOPE_MARGIN = 1.02;

  private final CarpoolStopIndex stopIndex;
  private final CarpoolRouter baselineRouter;
  private final double maxCarSpeedMetersPerSecond;
  private final Duration maxTripDuration;

  public CorridorBuilder(
    CarpoolStopIndex stopIndex,
    StreetLimitationParametersService streetLimitationParametersService,
    Duration maxTripDuration
  ) {
    this(
      stopIndex,
      new CarpoolStreetRouter(streetLimitationParametersService, maxTripDuration),
      streetLimitationParametersService.maxCarSpeed(),
      maxTripDuration
    );
  }

  /**
   * @param baselineRouter goal-directed router for the baseline legs
   * @param maxCarSpeedMetersPerSecond the graph's maximum car speed, which bounds the ellipses
   * @param maxTripDuration the longest trip that is modelled, which caps every leg's limit
   */
  public CorridorBuilder(
    CarpoolStopIndex stopIndex,
    CarpoolRouter baselineRouter,
    double maxCarSpeedMetersPerSecond,
    Duration maxTripDuration
  ) {
    this.stopIndex = Objects.requireNonNull(stopIndex, "stopIndex");
    this.baselineRouter = Objects.requireNonNull(baselineRouter, "baselineRouter");
    if (maxCarSpeedMetersPerSecond <= 0) {
      throw new IllegalArgumentException("maxCarSpeed must be positive");
    }
    this.maxCarSpeedMetersPerSecond = maxCarSpeedMetersPerSecond;
    this.maxTripDuration = Objects.requireNonNull(maxTripDuration, "maxTripDuration");
  }

  /**
   * The corridor of the trip, or {@code null} when a leg of its baseline cannot be routed within
   * the carpool bound: such a trip cannot carry a passenger.
   */
  @Nullable
  public CarpoolCorridor build(CarpoolTrip trip, List<Vertex> vertices) {
    int legs = vertices.size() - 1;

    var legDurations = new Duration[legs];
    for (int leg = 0; leg < legs; leg++) {
      var segment = baselineRouter.route(vertices.get(leg), vertices.get(leg + 1));
      if (segment == null) {
        LOG.debug("Leg {} of carpool trip {} cannot be routed; no corridor", leg, trip.getId());
        return null;
      }
      legDurations[leg] = segment.duration();
    }
    var legLimits = DriverLegLimits.legLimits(trip, legDurations, maxTripDuration);

    var stops = new ArrayList<CarpoolCorridor.CorridorStop>();
    for (int leg = 0; leg < legs; leg++) {
      var from = vertices.get(leg);
      var to = vertices.get(leg + 1);
      long bound = legLimits[leg].toSeconds();
      var envelope = ellipseEnvelope(from, to, bound, maxCarSpeedMetersPerSecond);

      var forward = CarpoolTreeStreetRouter.carTree(
        from,
        false,
        legLimits[leg],
        new EllipseBounds(to.getCoordinate(), bound, maxCarSpeedMetersPerSecond)
      );
      var reverse = CarpoolTreeStreetRouter.carTree(
        to,
        true,
        legLimits[leg],
        new EllipseBounds(from.getCoordinate(), bound, maxCarSpeedMetersPerSecond)
      );
      for (var stop : stopIndex.stopsWithin(envelope)) {
        var dropoff = times(stopIndex.dropoffSnap(stop.getId()), forward, reverse, bound);
        var pickup = times(stopIndex.pickupSnap(stop.getId()), forward, reverse, bound);
        if (dropoff != null || pickup != null) {
          stops.add(
            new CarpoolCorridor.CorridorStop(
              stop.getId(),
              leg,
              dropoff == null ? -1 : dropoff[0],
              dropoff == null ? -1 : dropoff[1],
              pickup == null ? -1 : pickup[0],
              pickup == null ? -1 : pickup[1]
            )
          );
        }
      }
    }
    LOG.debug(
      "Corridor of carpool trip {}: {} legs, {} stop entries",
      trip.getId(),
      legs,
      stops.size()
    );
    return new CarpoolCorridor(List.of(legDurations), List.of(legLimits), stops);
  }

  /**
   * Driving times {leg start → snap vertex, snap vertex → leg end}, or {@code null} when there is
   * no snap, a side is unreachable, or the detour through it exceeds the leg's bound.
   */
  @Nullable
  private static int[] times(
    @Nullable SnapResult snap,
    ShortestPathTree<State, Edge, Vertex> forward,
    ShortestPathTree<State, Edge, Vertex> reverse,
    long bound
  ) {
    if (snap == null) {
      return null;
    }
    int toStop = elapsedSeconds(forward, snap.vertex());
    int fromStop = elapsedSeconds(reverse, snap.vertex());
    if (toStop < 0 || fromStop < 0 || (long) toStop + fromStop > bound) {
      return null;
    }
    return new int[] { toStop, fromStop };
  }

  /** The tree's travel time to (or from) {@code vertex} in seconds, {@code -1} if unreached. */
  private static int elapsedSeconds(ShortestPathTree<State, Edge, Vertex> tree, Vertex vertex) {
    var state = tree.getState(vertex);
    return state == null ? -1 : (int) state.getElapsedTimeSeconds();
  }

  /**
   * A bounding box containing the ellipse with foci {@code from} and {@code to} whose
   * sum-of-distances is {@code boundSeconds} of driving at the maximum car speed: every point of
   * it lies within half that distance of the midpoint between the foci.
   */
  static Envelope ellipseEnvelope(Vertex from, Vertex to, long boundSeconds, double maxCarSpeed) {
    double halfMeters = ((boundSeconds * maxCarSpeed) / 2.0) * ENVELOPE_MARGIN;
    double midLat = (from.getLat() + to.getLat()) / 2.0;
    double midLon = (from.getLon() + to.getLon()) / 2.0;
    return SphericalDistanceLibrary.bounds(midLat, midLon, halfMeters, halfMeters);
  }
}
