package org.opentripplanner.ext.carpooling.routing;

import java.time.ZonedDateTime;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.astar.model.GraphPath;
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
   *        portions use the walks' own A* weights and are not multiplied by this.
   */
  public CarpoolAccessEgress(
    int stop,
    int passengerDepartureTime,
    InsertionCandidate insertionCandidate,
    TimeAndCost penalty,
    double carpoolReluctance
  ) {
    this.stop = stop;
    this.passengerDepartureTime = passengerDepartureTime;
    this.insertionCandidate = insertionCandidate;
    this.penalty = penalty;
    this.carpoolReluctance = carpoolReluctance;
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
      this.carpoolReluctance
    );
  }

  /**
   * Not implemented — Raptor never fetches the final State of a carpool access/egress leg.
   * Itinerary mapping reads the leg's segments and times via the public accessors instead. May be
   * implemented later if a caller starts requiring it.
   *
   * @throws UnsupportedOperationException always.
   */
  @Override
  public State getFinalState() {
    throw new UnsupportedOperationException(
      "Fetching last state of CarpoolAccessEgress is not yet implemented"
    );
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
