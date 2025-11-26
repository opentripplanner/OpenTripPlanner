package org.opentripplanner.street.search.request;

import static java.util.Objects.requireNonNull;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.astar.spi.AStarRequest;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.street.model.edge.ExtensionRequestContext;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;
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
  public static final int MAX_CLOSENESS_METERS = 500;

  // the time at which the search started
  private final Instant startTime;
  private final StreetMode mode;
  private final boolean arriveBy;
  private final boolean wheelchair;

  private final Envelope fromEnvelope;
  private final Envelope toEnvelope;

  private final boolean geoidElevation;

  private final double turnReluctance;
  private final WalkRequest walk;
  private final BikeRequest bike;
  private final ScooterRequest scooter;
  private final CarRequest car;
  private final WheelchairRequest wheelchairRequest;
  private final ElevatorRequest elevator;

  private IntersectionTraversalCalculator intersectionTraversalCalculator =
    IntersectionTraversalCalculator.DEFAULT;

  private List<ExtensionRequestContext> extensionRequestContexts;

  /**
   * Constructor only used for creating a default instance.
   */
  private StreetSearchRequest() {
    this.startTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    this.mode = StreetMode.WALK;
    this.arriveBy = false;
    this.wheelchair = false;
    this.fromEnvelope = null;
    this.toEnvelope = null;
    this.geoidElevation = false;
    this.turnReluctance = 1.0;
    this.walk = WalkRequest.DEFAULT;
    this.bike = BikeRequest.DEFAULT;
    this.scooter = ScooterRequest.DEFAULT;
    this.car = CarRequest.DEFAULT;
    this.wheelchairRequest = WheelchairRequest.DEFAULT;
    this.elevator = ElevatorRequest.DEFAULT;
  }

  StreetSearchRequest(StreetSearchRequestBuilder builder) {
    this.startTime = RouteRequest.normalizeDateTime(builder.startTimeOrNow());
    this.mode = builder.mode;
    this.arriveBy = builder.arriveBy;
    this.wheelchair = builder.wheelchairEnabled;
    this.fromEnvelope = builder.fromEnvelope;
    this.toEnvelope = builder.toEnvelope;
    this.geoidElevation = builder.geoidElevation;
    this.turnReluctance = builder.turnReluctance;
    this.walk = requireNonNull(builder.walk);
    this.bike = requireNonNull(builder.bike);
    this.scooter = requireNonNull(builder.scooter);
    this.car = requireNonNull(builder.car);
    this.wheelchairRequest = requireNonNull(builder.wheelchair);
    this.elevator = requireNonNull(builder.elevator);
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

  public boolean allowsArrivingInRentalAtDestination() {
    return Optional.ofNullable(rental(mode()))
      .map(RentalRequest::allowArrivingInRentedVehicleAtDestination)
      .orElse(false);
  }

  public WheelchairRequest wheelchair() {
    return wheelchairRequest;
  }

  public boolean geoidElevation() {
    return geoidElevation;
  }

  /**
   * The requested mode for the search. This contains information about all allowed transitions
   * between the different traverse modes, such as renting or parking a vehicle. Contrary to
   * currentMode, which can change when traversing edges, this is constant for a single search.
   */
  public StreetMode mode() {
    return mode;
  }

  public Envelope fromEnvelope() {
    return fromEnvelope;
  }

  public Envelope toEnvelope() {
    return toEnvelope;
  }

  public boolean arriveBy() {
    return arriveBy;
  }

  public boolean wheelchairEnabled() {
    return wheelchair;
  }

  public IntersectionTraversalCalculator intersectionTraversalCalculator() {
    return intersectionTraversalCalculator;
  }

  public List<ExtensionRequestContext> listExtensionRequestContexts() {
    return extensionRequestContexts;
  }

  public void setExtensionRequestContexts(List<ExtensionRequestContext> extensionRequestContexts) {
    this.extensionRequestContexts = extensionRequestContexts;
  }

  public StreetSearchRequestBuilder copyOfReversed(Instant time) {
    return copyOf(this).withStartTime(time).withArriveBy(!arriveBy);
  }

  public void setIntersectionTraversalCalculator(
    IntersectionTraversalCalculator intersectionTraversalCalculator
  ) {
    this.intersectionTraversalCalculator = intersectionTraversalCalculator;
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

  public WalkRequest walk() {
    return walk;
  }

  public BikeRequest bike() {
    return bike;
  }

  public CarRequest car() {
    return car;
  }

  public ScooterRequest scooter() {
    return scooter;
  }

  public double turnReluctance() {
    return turnReluctance;
  }

  public RentalRequest rental(TraverseMode traverseMode) {
    return switch (traverseMode) {
      case BICYCLE -> bike.rental();
      case SCOOTER -> scooter.rental();
      case CAR -> car.rental();
      case WALK, FLEX -> throw new IllegalArgumentException();
    };
  }

  public RentalRequest rental(StreetMode mode) {
    return switch (mode) {
      case BIKE_RENTAL -> bike.rental();
      case SCOOTER_RENTAL -> scooter.rental();
      case CAR_RENTAL -> car.rental();
      case NOT_SET,
        WALK,
        BIKE,
        BIKE_TO_PARK,
        CAR,
        CAR_TO_PARK,
        CAR_PICKUP,
        CAR_HAILING,
        FLEXIBLE -> null;
    };
  }

  public ElevatorRequest elevator() {
    return elevator;
  }

  /**
   * Get parking preferences for the traverse mode. Note, only car and bike are supported.
   */
  public ParkingRequest parking(TraverseMode mode) {
    return mode == TraverseMode.CAR ? car.parking() : bike.parking();
  }
}
