package org.opentripplanner.routing.core;

import java.time.Instant;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.ext.dataoverlay.routing.DataOverlayContext;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.routing.api.request.request.VehicleParkingRequest;
import org.opentripplanner.routing.api.request.request.VehicleRentalRequest;
import org.opentripplanner.routing.core.intersection_model.DrivingDirection;
import org.opentripplanner.routing.core.intersection_model.IntersectionTraversalCalculator;
import org.opentripplanner.routing.core.intersection_model.IntersectionTraversalModel;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.DominanceFunction;

public class AStarRequest {

  /**
   * How close to do you have to be to the start or end to be considered "close".
   *
   * @see AStarRequest#isCloseToStartOrEnd(Vertex)
   * @see DominanceFunction#betterOrEqualAndComparable(State, State)
   */
  private static final int MAX_CLOSENESS_METERS = 500;

  // the time at which the search started
  private final Instant startTime;
  private final RoutingPreferences preferences;
  private final StreetMode mode;
  private final boolean arriveBy;
  private final boolean wheelchair;
  private final VehicleParkingRequest parking;
  private final VehicleRentalRequest rental;

  private final Envelope fromEnvelope;
  private final Envelope toEnvelope;

  protected IntersectionTraversalCalculator intersectionTraversalCalculator = IntersectionTraversalCalculator.create(
    IntersectionTraversalModel.SIMPLE,
    DrivingDirection.RIGHT
  );

  protected DataOverlayContext dataOverlayContext;

  public AStarRequest(Instant startTime, RouteRequest opt, StreetMode mode) {
    this.startTime = startTime;
    this.preferences = opt.preferences();
    this.mode = mode;
    this.arriveBy = opt.arriveBy();
    this.wheelchair = opt.wheelchair();
    this.parking = opt.journey().parking();
    this.rental = opt.journey().rental();
    this.fromEnvelope =
      opt.from() != null && opt.from().getCoordinate() != null
        ? getEnvelope(opt.from().getCoordinate())
        : null;
    this.toEnvelope =
      opt.to() != null && opt.to().getCoordinate() != null
        ? getEnvelope(opt.to().getCoordinate())
        : null;
  }

  /**
   * This constructor is only to be used from the copyOfReversed method
   */
  private AStarRequest(AStarRequest original, Instant time) {
    this.startTime = time;
    this.preferences = original.preferences.clone();
    this.mode = original.mode;
    this.arriveBy = !original.arriveBy;
    this.wheelchair = original.wheelchair;
    this.parking = original.parking.clone();
    this.rental = original.rental.clone();
    this.fromEnvelope = original.toEnvelope;
    this.toEnvelope = original.fromEnvelope;
  }

  public Instant startTime() {
    return startTime;
  }

  public RoutingPreferences preferences() {
    return preferences;
  }

  /**
   * The requested mode for the search. This contains information about all allowed transitions
   * between the different traverse modes, such as renting or parking a vehicle. Contrary to
   * currentMode, which can change when traversing edges, this is constant for a single search.
   */
  public StreetMode mode() {
    return mode;
  }

  public boolean arriveBy() {
    return arriveBy;
  }

  public boolean wheelchair() {
    return wheelchair;
  }

  public VehicleParkingRequest parking() {
    return parking;
  }

  public VehicleRentalRequest rental() {
    return rental;
  }

  public AStarRequest copyOfReversed(Instant time) {
    return new AStarRequest(this, time);
  }

  public void setIntersectionTraversalCalculator(
    IntersectionTraversalCalculator intersectionTraversalCalculator
  ) {
    this.intersectionTraversalCalculator = intersectionTraversalCalculator;
  }

  public void setDataOverlayContext(DataOverlayContext dataOverlayContext) {
    this.dataOverlayContext = dataOverlayContext;
  }

  /**
   * Returns if the vertex is considered "close" to the start or end point of the request. This is
   * useful if you want to allow loops in car routes under certain conditions.
   * <p>
   * Note: If you are doing Raptor access/egress searches this method does not take the possible
   * intermediate points (stations) into account. This means that stations might be skipped because
   * a car route to it cannot be found and a suboptimal route to another station is returned
   * instead.
   * <p>
   * If you encounter a case of this, you can adjust this code to take this into account.
   *
   * @see AStarRequest#MAX_CLOSENESS_METERS
   * @see DominanceFunction#betterOrEqualAndComparable(State, State)
   */
  public boolean isCloseToStartOrEnd(Vertex vertex) {
    return (
      (fromEnvelope != null && fromEnvelope.intersects(vertex.getCoordinate())) ||
      (toEnvelope != null && toEnvelope.intersects(vertex.getCoordinate()))
    );
  }

  private static Envelope getEnvelope(Coordinate c) {
    double lat = SphericalDistanceLibrary.metersToDegrees(MAX_CLOSENESS_METERS);
    double lon = SphericalDistanceLibrary.metersToLonDegrees(MAX_CLOSENESS_METERS, c.y);

    Envelope env = new Envelope(c);
    env.expandBy(lon, lat);

    return env;
  }
}
