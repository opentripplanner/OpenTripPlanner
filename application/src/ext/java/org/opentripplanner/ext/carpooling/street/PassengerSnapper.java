package org.opentripplanner.ext.carpooling.street;

import java.time.Duration;
import org.jspecify.annotations.Nullable;
import org.opentripplanner.ext.carpooling.street.CarReachableVertexSnapper.SnapResult;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Finds where a car can stop for the passenger of one request: links the passenger's coordinate to
 * the street graph and snaps it to a car-reachable vertex within the walk budget.
 */
public class PassengerSnapper {

  private static final Logger LOG = LoggerFactory.getLogger(PassengerSnapper.class);

  private final TemporaryVertexFactory vertexFactory;
  private final CarReachableVertexSnapper carReachableVertexSnapper;
  private final StreetSearchRequest streetSearchRequest;
  private final Duration maxWalk;

  public PassengerSnapper(
    TemporaryVertexFactory vertexFactory,
    CarReachableVertexSnapper carReachableVertexSnapper,
    StreetSearchRequest streetSearchRequest,
    Duration maxWalk
  ) {
    this.vertexFactory = vertexFactory;
    this.carReachableVertexSnapper = carReachableVertexSnapper;
    this.streetSearchRequest = streetSearchRequest;
    this.maxWalk = maxWalk;
  }

  /** Where a car can pick up a passenger walking from {@code origin}, or {@code null}. */
  @Nullable
  public SnapResult snapPickup(WgsCoordinate origin) {
    return snap(origin, true);
  }

  /** Where a car can drop off a passenger walking to {@code destination}, or {@code null}. */
  @Nullable
  public SnapResult snapDropoff(WgsCoordinate destination) {
    return snap(destination, false);
  }

  @Nullable
  private SnapResult snap(WgsCoordinate coordinate, boolean pickup) {
    var vertex = vertexFactory.createPassengerVertex(coordinate);
    if (vertex == null) {
      LOG.info("Could not link passenger coordinates {} to graph", coordinate);
      return null;
    }
    var snap = pickup
      ? carReachableVertexSnapper.snapPickup(streetSearchRequest, vertex, maxWalk)
      : carReachableVertexSnapper.snapDropoff(streetSearchRequest, vertex, maxWalk);
    if (snap == null) {
      LOG.debug(
        "No car-reachable vertex reachable within {} from passenger coords {}",
        maxWalk,
        coordinate
      );
    }
    return snap;
  }
}
