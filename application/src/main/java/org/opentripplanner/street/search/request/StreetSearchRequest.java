package org.opentripplanner.street.search.request;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.astar.spi.AStarRequest;
import org.opentripplanner.ext.dataoverlay.routing.DataOverlayContext;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.intersection_model.IntersectionTraversalCalculator;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.strategy.DominanceFunctions;

/**
 * This class contains all information from the {@link RouteRequest} class required for an A* search
 */
public class StreetSearchRequest implements AStarRequest {

  private static final StreetSearchRequest DEFAULT = new StreetSearchRequest();

  /**
   * How close to do you have to be to the start or end to be considered "close".
   *
   * @see StreetSearchRequest#isCloseToStartOrEnd(Vertex)
   * @see DominanceFunctions#betterOrEqualAndComparable(State, State)
   */
  private static final int MAX_CLOSENESS_METERS = 500;

  // the time at which the search started
  private final Instant startTime;
  private final RoutingPreferences preferences;
  private final StreetMode mode;
  private final boolean arriveBy;
  private final boolean wheelchair;

  private final GenericLocation from;
  private final Envelope fromEnvelope;
  private final GenericLocation to;
  private final Envelope toEnvelope;

  private IntersectionTraversalCalculator intersectionTraversalCalculator =
    IntersectionTraversalCalculator.DEFAULT;

  private DataOverlayContext dataOverlayContext;

  /**
   * Constructor only used for creating a default instance.
   */
  private StreetSearchRequest() {
    this.startTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    this.preferences = RoutingPreferences.DEFAULT;
    this.mode = StreetMode.WALK;
    this.arriveBy = false;
    this.wheelchair = false;
    this.from = null;
    this.fromEnvelope = null;
    this.to = null;
    this.toEnvelope = null;
  }

  StreetSearchRequest(StreetSearchRequestBuilder builder) {
    this.startTime = RouteRequest.normalizeDateTime(builder.startTimeOrNow());
    this.preferences = builder.preferences;
    this.mode = builder.mode;
    this.arriveBy = builder.arriveBy;
    this.wheelchair = builder.wheelchair;
    this.from = builder.from;
    this.fromEnvelope = createEnvelope(from);
    this.to = builder.to;
    this.toEnvelope = createEnvelope(to);
  }

  public static StreetSearchRequestBuilder of() {
    return new StreetSearchRequestBuilder(DEFAULT).withStartTime(Instant.now());
  }

  public static StreetSearchRequestBuilder copyOf(StreetSearchRequest original) {
    return new StreetSearchRequestBuilder(original);
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

  public GenericLocation from() {
    return from;
  }

  public GenericLocation to() {
    return to;
  }

  public IntersectionTraversalCalculator intersectionTraversalCalculator() {
    return intersectionTraversalCalculator;
  }

  public DataOverlayContext dataOverlayContext() {
    return dataOverlayContext;
  }

  public StreetSearchRequestBuilder copyOfReversed(Instant time) {
    return copyOf(this).withStartTime(time).withArriveBy(!arriveBy);
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
   * @see StreetSearchRequest#MAX_CLOSENESS_METERS
   * @see DominanceFunctions#betterOrEqualAndComparable(State, State)
   */
  public boolean isCloseToStartOrEnd(Vertex vertex) {
    return (
      (fromEnvelope != null && fromEnvelope.intersects(vertex.getCoordinate())) ||
      (toEnvelope != null && toEnvelope.intersects(vertex.getCoordinate()))
    );
  }

  @Nullable
  private static Envelope createEnvelope(GenericLocation location) {
    if (location == null) {
      return null;
    }

    Coordinate coordinate = location.getCoordinate();
    if (coordinate == null) {
      return null;
    }

    double lat = SphericalDistanceLibrary.metersToDegrees(MAX_CLOSENESS_METERS);
    double lon = SphericalDistanceLibrary.metersToLonDegrees(MAX_CLOSENESS_METERS, coordinate.y);

    Envelope env = new Envelope(coordinate);
    env.expandBy(lon, lat);

    return env;
  }
}
