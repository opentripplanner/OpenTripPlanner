package org.opentripplanner.ext.carpooling.routing;

import java.time.ZonedDateTime;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.ext.carpooling.util.GraphPathUtils;
import org.opentripplanner.framework.model.TimeAndCost;
import org.opentripplanner.raptor.spi.RaptorConstants;
import org.opentripplanner.raptor.spi.RaptorCostConverter;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RoutingAccessEgress;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;

/**
 * Raptor access/egress adapter wrapping an {@link InsertionCandidate}: the candidate carries the
 * carpool-side data (walks, shared segments, ride duration) and this class adds the Raptor framing
 * (stop index, departure time anchor, c1, penalty, reluctance).
 * <p>
 * The walk paths' A* weights are used as-is for the walk portion of the cost; they already encode
 * the user's walk preferences (reluctance, safety, slope, ...) from the search that produced them.
 * The ride portion is weighted by {@code carpoolReluctance}.
 */
public class CarpoolAccessEgress implements RoutingAccessEgress {

  private final int stop;

  /**
   * The Raptor departure time of this access/egress leg, in seconds since
   * {@code transitSearchTimeZero}. This is the moment the passenger leaves the origin side of the
   * leg: the start of an optional walk to the carpool pickup, or — when no walk is needed — the
   * moment the carpool itself arrives at the pickup. The driver runs on a committed schedule, so
   * Raptor cannot delay this time.
   */
  private final int passengerDepartureTime;

  /**
   * The Raptor arrival time of this access/egress leg, in seconds since
   * {@code transitSearchTimeZero}. This is the moment the passenger reaches the destination side
   * of the leg: the end of an optional walk from the carpool dropoff, or — when no walk is needed
   * — the moment the carpool reaches the dropoff. Equal to {@code passengerDepartureTime +
   * walkToPickupSeconds + rideSeconds + walkFromDropoffSeconds}.
   */
  private final int passengerArrivalTime;

  private final int durationInSeconds;
  private final int c1;
  private final int timePenalty;

  private final InsertionCandidate insertionCandidate;
  private final TimeAndCost penalty;
  private final double carpoolReluctance;

  private final EndpointLabel startLabel;
  private final EndpointLabel endLabel;

  /**
   * @param stop Raptor stop index of the transit-side endpoint — the stop the passenger boards
   *        transit at (for access) or alights from transit at (for egress).
   * @param passengerDepartureTime see {@link #passengerDepartureTime}.
   * @param insertionCandidate the carpool-side data: walks bracketing the ride, the shared ride
   *        itself, the trip and pickup/dropoff positions. Everything except Raptor framing flows
   *        from here.
   * @param penalty optional Raptor time/cost penalty added on top of the leg, applied via
   *        {@link #withPenalty(TimeAndCost)}; pass {@link TimeAndCost#ZERO} for no penalty.
   * @param carpoolReluctance multiplier on ride seconds when computing {@link #c1()}; the walk
   *        portions are billed at the walks' own A* weights and are not multiplied by this.
   * @param startLabel label data for the first leg's {@code from} place. For an access this is
   *        the passenger origin ({@link EndpointLabel#forLocation(org.opentripplanner.model.GenericLocation)});
   *        for an egress it is the transit stop the passenger alighted from
   *        ({@link EndpointLabel#forStop(org.opentripplanner.transit.model.site.StopLocation)}).
   *        The mapper resolves the label into a {@code Place}.
   * @param endLabel symmetric to {@code startLabel}: label data for the last leg's {@code to}
   *        place. Transit stop for an access, passenger destination for an egress.
   */
  public CarpoolAccessEgress(
    int stop,
    int passengerDepartureTime,
    InsertionCandidate insertionCandidate,
    TimeAndCost penalty,
    double carpoolReluctance,
    EndpointLabel startLabel,
    EndpointLabel endLabel
  ) {
    this.stop = stop;
    this.passengerDepartureTime = passengerDepartureTime;
    this.insertionCandidate = insertionCandidate;
    this.penalty = penalty;
    this.carpoolReluctance = carpoolReluctance;
    this.startLabel = startLabel;
    this.endLabel = endLabel;
    this.timePenalty = penalty.isZero() ? RaptorConstants.TIME_NOT_SET : penalty.timeInSeconds();

    var walkToPickup = insertionCandidate.walkToPickup();
    var walkFromDropoff = insertionCandidate.walkFromDropoff();
    int walkSeconds = (int) (GraphPathUtils.durationOrZero(walkToPickup).getSeconds() +
      GraphPathUtils.durationOrZero(walkFromDropoff).getSeconds());
    int rideSeconds = (int) insertionCandidate.getPassengerRideDuration().getSeconds();
    this.durationInSeconds = walkSeconds + rideSeconds;
    this.passengerArrivalTime = passengerDepartureTime + this.durationInSeconds;

    double walkWeight =
      GraphPathUtils.weightOrZero(walkToPickup) + GraphPathUtils.weightOrZero(walkFromDropoff);
    double totalWeight = walkWeight + insertionCandidate.getPassengerRideWeight(carpoolReluctance);
    this.c1 = RaptorCostConverter.toRaptorCost(totalWeight) + penalty.cost().toCentiSeconds();
  }

  @Override
  public int stop() {
    return this.stop;
  }

  /**
   * The Raptor cost of this leg, equal to {@code walkToPickupWeight + walkFromDropoffWeight +
   * rideSeconds * carpoolReluctance}, converted to Raptor's centi-second unit. The walk weights
   * already encode the user's walk preferences (reluctance, safety, slope, ...) — only the ride
   * portion is multiplied by {@code carpoolReluctance}.
   */
  @Override
  public int c1() {
    return this.c1;
  }

  /**
   * Total wall-clock duration of the leg (walk + ride + walk), in seconds. Constant — Raptor
   * cannot stretch or shrink this since the carpool runs on a fixed schedule.
   */
  @Override
  public int durationInSeconds() {
    return this.durationInSeconds;
  }

  @Override
  public int timePenalty() {
    return this.timePenalty;
  }

  /**
   * The carpool's departure time is fixed by the driver's schedule. Returns
   * {@link #passengerDepartureTime} if the requested time is at or before it, otherwise
   * {@link RaptorConstants#TIME_NOT_SET} — the passenger can't be picked up later than the
   * driver passes.
   */
  @Override
  public int earliestDepartureTime(int requestedDepartureTime) {
    if (requestedDepartureTime > passengerDepartureTime) {
      return RaptorConstants.TIME_NOT_SET;
    }
    return passengerDepartureTime;
  }

  /**
   * Symmetric to {@link #earliestDepartureTime(int)}: the carpool's arrival time is fixed.
   * Returns {@link #passengerArrivalTime} if the requested time is at or after it, otherwise
   * {@link RaptorConstants#TIME_NOT_SET}.
   */
  @Override
  public int latestArrivalTime(int requestedArrivalTime) {
    if (requestedArrivalTime < passengerArrivalTime) {
      return RaptorConstants.TIME_NOT_SET;
    }
    return passengerArrivalTime;
  }

  /**
   * Always {@code true}: a carpool leg has a fixed time window because the driver runs on a
   * committed schedule, so Raptor must respect the leg's departure/arrival times rather than
   * shifting the leg to fit the request.
   */
  @Override
  public boolean hasOpeningHours() {
    return true;
  }

  /** See {@link #passengerDepartureTime} for semantics. */
  public int getPassengerDepartureTime() {
    return passengerDepartureTime;
  }

  /** See {@link #passengerArrivalTime} for semantics. */
  public int getPassengerArrivalTime() {
    return passengerArrivalTime;
  }

  /**
   * Returns a copy of this leg with the given Raptor penalty applied on top. Used by Raptor's
   * access-egress penalty pass; everything else (stop, time anchor, candidate, reluctance) is
   * preserved unchanged. The penalty's cost is folded into {@link #c1()} and its time is exposed
   * via {@link #timePenalty()}, mirroring {@code DefaultAccessEgress}.
   */
  @Override
  public RoutingAccessEgress withPenalty(TimeAndCost penalty) {
    if (this.penalty != TimeAndCost.ZERO) {
      throw new IllegalStateException("Can not add penalty twice...");
    }
    return new CarpoolAccessEgress(
      this.stop,
      this.passengerDepartureTime,
      this.insertionCandidate,
      penalty,
      this.carpoolReluctance,
      this.startLabel,
      this.endLabel
    );
  }

  /**
   * The underlying carpool trip — i.e. the driver's committed route, schedule, and public-contact
   * details. Exposed for the itinerary mapper, which reads trip-level metadata (start time,
   * {@code publicContactInformation}, ...) to build the carpool leg's {@code pickupBookingInfo}.
   * Mirrors how the direct path reads the same data straight off the {@link InsertionCandidate}.
   */
  public CarpoolTrip trip() {
    return insertionCandidate.trip();
  }

  /**
   * Label data for the first leg's {@code from} place. For an access this carries the passenger
   * origin; for an egress, the transit stop the passenger alighted from. Read by the itinerary
   * mapper to name the chain's outermost start endpoint.
   */
  public EndpointLabel startLabel() {
    return startLabel;
  }

  /**
   * Symmetric to {@link #startLabel()}: label data for the last leg's {@code to} place. Transit
   * stop for an access, passenger destination for an egress.
   */
  public EndpointLabel endLabel() {
    return endLabel;
  }

  /**
   * The A* state sitting at the transit-stop endpoint of the passenger leg.
   * <p>
   * The transit stop is the chain's far end — its end for an access (passenger origin → … → stop)
   * and its start for an egress (stop → … → passenger destination). The transit side is identified
   * from the labels, which carry the stop on the end label for an access and on the start label
   * for an egress; the returned state is the segment endpoint touching the stop. When neither
   * label carries a stop (e.g. a direct itinerary), the chain end is used.
   *
   * <h4>Limitation: this state does not head a full-leg pointer chain</h4>
   * <p>
   * The {@link RoutingAccessEgress#getFinalState()} contract expects the returned state to head a
   * {@link State#getBackState()} chain that reconstructs the <em>entire</em> street search — that
   * is the assumption behind callers such as {@code RaptorPathToItineraryMapper}, which wrap the
   * state in a {@link org.opentripplanner.street.model.path.StreetPath} to rebuild the leg.
   * <p>
   * A carpool leg does not honour that assumption today. It is not one A* search but several
   * independent ones stitched together in the domain layer: an optional walk to the pickup, the
   * shared ride (segments taken off the driver's committed route), and an optional walk from the
   * dropoff. These chains are not linked — the first state of each segment is a fresh search
   * origin with a {@code null} back state, and nothing points across the pickup/dropoff vertices
   * from one segment into the next. Following {@code getBackState()} from the returned state
   * therefore reconstructs only the single terminal segment, not the whole walk + ride + walk leg.
   * Splicing the segments into one continuous back-state chain is possible but not yet done, since
   * no caller needs it.
   * <p>
   * This is sound only because no caller reconstructs a carpool leg from this state. Itinerary
   * mapping for carpool legs is routed through {@link org.opentripplanner.ext.carpooling.internal.CarpoolItineraryMapper}
   * instead, which rebuilds the WALK + CARPOOL + WALK legs directly from the segments and times
   * via the public accessors. The state returned here is consumed only for the
   * direction-independent scalar checks performed on every access/egress — notably
   * {@link State#isRentingVehicleFromStation()}, which is always {@code false} since a passenger
   * rides in the driver's car rather than a station-rented vehicle. Do not wrap this state in a
   * {@code StreetPath} expecting the full leg; that path would silently omit the ride and the
   * other walk.
   */
  @Override
  public State getFinalState() {
    if (startLabel.stop() != null) {
      var firstSegment = walkToPickup() != null ? walkToPickup() : sharedSegments().getFirst();
      return firstSegment.states.getFirst();
    }
    var lastSegment = walkFromDropoff() != null ? walkFromDropoff() : sharedSegments().getLast();
    return lastSegment.states.getLast();
  }

  /** Always {@code false}: a carpool leg, by definition, contains a vehicle ride. */
  @Override
  public boolean isWalkOnly() {
    return false;
  }

  /** {@inheritDoc} */
  @Override
  public TimeAndCost penalty() {
    return this.penalty;
  }

  /**
   * Walk path from the passenger's origin (or transit stop, for egress) to the snapped pickup
   * vertex. {@code null} when the origin/stop is already on a car-accessible vertex and no walk
   * is needed.
   */
  @Nullable
  public GraphPath<State, Edge, Vertex> walkToPickup() {
    return insertionCandidate.walkToPickup();
  }

  /**
   * The carpool route segments traversed by the passenger between pickup and dropoff (inclusive of
   * intermediate stops the driver makes along the way for other passengers). Never empty for a
   * valid leg.
   */
  public List<GraphPath<State, Edge, Vertex>> sharedSegments() {
    return insertionCandidate.getSharedSegments();
  }

  /**
   * Walk path from the snapped dropoff vertex to the passenger's destination (or transit stop, for
   * access). {@code null} when the destination/stop is already on a car-accessible vertex and no
   * walk is needed.
   */
  @Nullable
  public GraphPath<State, Edge, Vertex> walkFromDropoff() {
    return insertionCandidate.walkFromDropoff();
  }

  /**
   * Absolute wall-clock time at which the carpool arrives at the pickup vertex — i.e. the start
   * of the shared ride. Computed from the driver's trip start time plus the duration of all
   * segments preceding the passenger's pickup; independent of {@code transitSearchTimeZero}.
   */
  public ZonedDateTime getCarpoolStart() {
    return insertionCandidate
      .trip()
      .startTime()
      .plus(insertionCandidate.getDurationUntilPickupArrival());
  }

  /** Absolute wall-clock time at which the carpool arrives at the dropoff vertex. */
  public ZonedDateTime getCarpoolEnd() {
    return getCarpoolStart().plus(insertionCandidate.getPassengerRideDuration());
  }

  /**
   * Cost of the carpool ride portion only (excluding walks and penalty), in raw weight units. The
   * itinerary mapper uses this for {@code CarpoolLeg.generalizedCost} so the displayed cost agrees
   * with the ride contribution to {@link #c1()}.
   */
  public double getPassengerRideWeight() {
    return insertionCandidate.getPassengerRideWeight(carpoolReluctance);
  }
}
