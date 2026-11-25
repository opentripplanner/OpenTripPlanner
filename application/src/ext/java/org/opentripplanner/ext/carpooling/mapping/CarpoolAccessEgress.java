package org.opentripplanner.ext.carpooling.mapping;

import java.time.ZonedDateTime;
import org.opentripplanner.ext.carpooling.routing.InsertionCandidate;
import org.opentripplanner.ext.carpooling.street.StreetPathUtils;
import org.opentripplanner.framework.model.TimeAndCost;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.raptor.spi.RaptorConstants;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressType;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RoutingAccessEgress;
import org.opentripplanner.routing.cost.CostLimit;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.utils.time.TimeUtils;

/**
 * Raptor access/egress adapter wrapping an {@link InsertionCandidate}: the candidate carries the
 * carpool-side data (walks, shared segments, ride duration) and this class adds the Raptor framing
 * (stop index, departure time anchor, c1, penalty, reluctance, board cost).
 * <p>
 * The walk paths' A* weights are used as-is for the walk portion of the cost; they already
 * encode the user's walk preferences (reluctance, safety, slope, ...) from the search that produced
 * them. The ride portion is weighted by {@code carpoolReluctance}, plus a fixed {@code boardCost}.
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
   * {@code transitSearchTimeZero}. This is the moment the passenger reaches the destination side of
   * the leg: the end of an optional walk from the carpool dropoff, or — when no walk is needed —
   * the moment the carpool reaches the dropoff. Equal to {@code passengerDepartureTime +
   * walkToPickupSeconds + rideSeconds + walkFromDropoffSeconds}.
   */
  private final int passengerArrivalTime;

  private final int durationInSeconds;
  private final int c1;
  private final int timePenalty;

  private final InsertionCandidate insertionCandidate;
  private final TimeAndCost penalty;
  private final double carpoolReluctance;
  private final int boardCost;

  private final EndpointLabel startLabel;
  private final EndpointLabel endLabel;

  /**
   * @param stop                   the Raptor index of the transit stop the leg connects to
   * @param passengerDepartureTime see {@link #passengerDepartureTime}
   * @param penalty                the Raptor penalty on the leg, {@link TimeAndCost#ZERO} for none
   * @param carpoolReluctance      the cost of a second of the ride; the walks keep their own weights
   * @param boardCost              the cost of being picked up, added once
   * @param startLabel             names the itinerary's start: the passenger's origin for access,
   *                               the transit stop for egress
   * @param endLabel               names the itinerary's end: the transit stop for access, the
   *                               passenger's destination for egress
   */
  public CarpoolAccessEgress(
    int stop,
    int passengerDepartureTime,
    InsertionCandidate insertionCandidate,
    TimeAndCost penalty,
    double carpoolReluctance,
    int boardCost,
    EndpointLabel startLabel,
    EndpointLabel endLabel
  ) {
    this.stop = stop;
    this.passengerDepartureTime = passengerDepartureTime;
    this.insertionCandidate = insertionCandidate;
    this.penalty = penalty;
    this.carpoolReluctance = carpoolReluctance;
    this.boardCost = boardCost;
    this.startLabel = startLabel;
    this.endLabel = endLabel;
    this.timePenalty = penalty.isZero() ? RaptorConstants.TIME_NOT_SET : penalty.timeInSeconds();

    var walkToPickup = insertionCandidate.walkToPickup();
    var walkFromDropoff = insertionCandidate.walkFromDropoff();
    int walkSeconds = (int) (StreetPathUtils.durationOrZero(walkToPickup).getSeconds() +
      StreetPathUtils.durationOrZero(walkFromDropoff).getSeconds());
    int rideSeconds = (int) insertionCandidate.getPassengerRideDuration().getSeconds();
    this.durationInSeconds = walkSeconds + rideSeconds;
    this.passengerArrivalTime = passengerDepartureTime + this.durationInSeconds;

    double walkWeight =
      StreetPathUtils.weightOrZero(walkToPickup) + StreetPathUtils.weightOrZero(walkFromDropoff);
    double totalWeight =
      walkWeight + insertionCandidate.getPassengerRideWeight(carpoolReluctance, boardCost);
    this.c1 = CostLimit.toRaptorCost(totalWeight) + penalty.cost().toCentiSeconds();
  }

  /**
   * The access or egress leg of an insertion whose ride runs between the passenger and a transit
   * stop.
   *
   * @param passengerLocation     the request's origin for access, its destination for egress
   * @param transitSearchTimeZero the time the leg's Raptor times are relative to
   */
  public static CarpoolAccessEgress of(
    InsertionCandidate insertionCandidate,
    AccessEgressType accessOrEgress,
    GenericLocation passengerLocation,
    ZonedDateTime transitSearchTimeZero,
    double carpoolReluctance,
    int boardCost
  ) {
    var passengerStartTime = insertionCandidate
      .pickupTime()
      .minus(StreetPathUtils.durationOrZero(insertionCandidate.walkToPickup()));
    var passengerDepartureTime = TimeUtils.toTransitTimeSeconds(
      transitSearchTimeZero,
      passengerStartTime.toInstant()
    );

    StopLocation transitStop = insertionCandidate.transitStop();
    EndpointLabel stopLabel = EndpointLabel.forStop(transitStop);
    EndpointLabel passengerLabel = EndpointLabel.forLocation(passengerLocation);

    return new CarpoolAccessEgress(
      transitStop.getIndex(),
      passengerDepartureTime,
      insertionCandidate,
      TimeAndCost.ZERO,
      carpoolReluctance,
      boardCost,
      accessOrEgress.isAccess() ? passengerLabel : stopLabel,
      accessOrEgress.isAccess() ? stopLabel : passengerLabel
    );
  }

  @Override
  public int stop() {
    return this.stop;
  }

  /**
   * The Raptor cost of this leg, equal to {@code walkToPickupWeight + walkFromDropoffWeight +
   * rideSeconds * carpoolReluctance + boardCost + penalty.cost()}, converted to Raptor's
   * centi-second unit. The walk weights already encode the user's walk preferences (reluctance,
   * safety, slope, ...) — only the ride portion is multiplied by {@code carpoolReluctance}.
   */
  @Override
  public int c1() {
    return this.c1;
  }

  /**
   * Total wall-clock duration of the leg (walk + ride + walk), in seconds. Constant — Raptor cannot
   * stretch or shrink this since the carpool runs on a fixed schedule.
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
   * {@link RaptorConstants#TIME_NOT_SET} — the passenger can't be picked up later than the driver
   * passes.
   */
  @Override
  public int earliestDepartureTime(int requestedDepartureTime) {
    if (requestedDepartureTime > passengerDepartureTime) {
      return RaptorConstants.TIME_NOT_SET;
    }
    return passengerDepartureTime;
  }

  /**
   * Symmetric to {@link #earliestDepartureTime(int)}: the carpool's arrival time is fixed. Returns
   * {@link #passengerArrivalTime} if the requested time is at or after it, otherwise
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
   * Returns a copy of this leg with the given Raptor penalty applied on top.
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
      this.boardCost,
      this.startLabel,
      this.endLabel
    );
  }

  /** The carpool-side data of this leg: the trip, the walks bracketing the ride and the ride. */
  public InsertionCandidate insertionCandidate() {
    return insertionCandidate;
  }

  public EndpointLabel startLabel() {
    return startLabel;
  }

  public EndpointLabel endLabel() {
    return endLabel;
  }

  /** Always {@code false}: a passenger rides in the driver's car, not a station-rented vehicle. */
  @Override
  public boolean isRentingVehicleFromStation() {
    return false;
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
   * Cost of the carpool ride portion only (excluding walks and penalty), in raw weight units. The
   * itinerary mapper uses this for {@code CarpoolLeg.generalizedCost} so the displayed cost agrees
   * with the ride contribution to {@link #c1()}.
   */
  public double getPassengerRideWeight() {
    return insertionCandidate.getPassengerRideWeight(carpoolReluctance, boardCost);
  }
}
