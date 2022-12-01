package org.opentripplanner.model.plan;

import static java.util.Locale.ROOT;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.opentripplanner.ext.flex.FlexibleTransitLeg;
import org.opentripplanner.framework.lang.DoubleUtils;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.model.SystemNotice;
import org.opentripplanner.raptor.api.path.PathStringBuilder;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.preference.ItineraryFilterPreferences;
import org.opentripplanner.routing.core.ItineraryFares;

/**
 * An Itinerary is one complete way of getting from the start location to the end location.
 */
public class Itinerary {

  /* final primitive properties */
  private final Duration duration;
  private final Duration transitDuration;
  private final int numberOfTransfers;
  private final Duration waitingDuration;
  private final double nonTransitDistanceMeters;
  private final boolean walkOnly;
  private final boolean streetOnly;
  private final Duration nonTransitDuration;

  /* mutable primitive properties */
  private Double elevationLost = 0.0;
  private Double elevationGained = 0.0;
  private int generalizedCost = -1;
  private int waitTimeOptimizedCost = -1;
  private int transferPriorityCost = -1;
  private boolean tooSloped = false;
  private Double maxSlope = null;
  private boolean arrivedAtDestinationWithRentedVehicle = false;

  /* Sandbox experimental properties */
  private Float accessibilityScore;

  /* other properties */

  private final List<SystemNotice> systemNotices = new ArrayList<>();
  private List<Leg> legs;

  private ItineraryFares fare = ItineraryFares.empty();

  public Itinerary(List<Leg> legs) {
    setLegs(legs);

    // Set aggregated data
    ItinerariesCalculateLegTotals totals = new ItinerariesCalculateLegTotals(legs);
    this.duration = totals.totalDuration;
    this.numberOfTransfers = totals.transfers();
    this.transitDuration = totals.transitDuration;
    this.nonTransitDuration = totals.nonTransitDuration;
    this.nonTransitDistanceMeters = DoubleUtils.roundTo2Decimals(totals.nonTransitDistanceMeters);
    this.waitingDuration = totals.walkingDuration;
    this.walkOnly = totals.walkOnly;
    this.streetOnly = totals.streetOnly;
    this.setElevationGained(totals.totalElevationGained);
    this.setElevationLost(totals.totalElevationLost);
  }

  /**
   * Used to convert a list of itineraries to a SHORT human readable string.
   *
   * @see #toStr()
   * <p>
   * It is great for comparing lists of itineraries in a test: {@code
   * assertEquals(toStr(List.of(it1)), toStr(result))}.
   */
  public static String toStr(List<Itinerary> list) {
    return list.stream().map(Itinerary::toStr).collect(Collectors.joining(", "));
  }

  /**
   * Time that the trip departs.
   */
  public ZonedDateTime startTime() {
    return firstLeg().getStartTime();
  }

  /**
   * Time that the trip arrives.
   */
  public ZonedDateTime endTime() {
    return lastLeg().getEndTime();
  }

  /**
   * Reflects the departureDelay on the first Leg Unit: seconds.
   */
  public int departureDelay() {
    return firstLeg().getDepartureDelay();
  }

  /**
   * Reflects the arrivalDelay on the last Leg Unit: seconds.
   */
  public int arrivalDelay() {
    return lastLeg().getArrivalDelay();
  }

  /**
   * This is the amount of time used to travel. {@code waitingTime} is NOT included.
   */
  public Duration effectiveDuration() {
    return getTransitDuration().plus(getNonTransitDuration());
  }

  /**
   * Total distance in meters.
   */
  public double distanceMeters() {
    return getLegs().stream().mapToDouble(Leg::getDistanceMeters).sum();
  }

  /**
   * Return {@code true} if all legs are WALKING.
   */
  public boolean isWalkingAllTheWay() {
    return isWalkOnly();
  }

  /**
   * Return {@code true} if all legs are WALKING.
   */
  public boolean isOnStreetAllTheWay() {
    return isStreetOnly();
  }

  /** TRUE if at least one leg is a transit leg. */
  public boolean hasTransit() {
    return legs
      .stream()
      .anyMatch(l -> l instanceof ScheduledTransitLeg || l instanceof FlexibleTransitLeg);
  }

  public Leg firstLeg() {
    return getLegs().get(0);
  }

  public Leg lastLeg() {
    return getLegs().get(getLegs().size() - 1);
  }

  /** Get the first transit leg if one exist */
  public Optional<Leg> firstTransitLeg() {
    return getLegs().stream().filter(TransitLeg.class::isInstance).findFirst();
  }

  /**
   * An itinerary can be flagged for removal with a system notice.
   * <p>
   * For example when tuning or manually testing the itinerary-filter-chain it you can enable {@link
   * ItineraryFilterPreferences#debug} and instead of
   * removing itineraries from the result the itineraries will be tagged by the filters instead.
   * This enables investigating, why an expected itinerary is missing from the result set. It can be
   * also used by other filters to see the already filtered itineraries.
   */
  public void flagForDeletion(SystemNotice notice) {
    systemNotices.add(notice);
  }

  /**
   * Remove all deletion flags of this itinerary, in effect undeleting it from the result.
   */
  public void removeDeletionFlags() {
    systemNotices.clear();
  }

  public boolean isFlaggedForDeletion() {
    return !getSystemNotices().isEmpty();
  }

  public Itinerary withTimeShiftToStartAt(ZonedDateTime afterTime) {
    Duration duration = Duration.between(firstLeg().getStartTime(), afterTime);
    List<Leg> timeShiftedLegs = getLegs()
      .stream()
      .map(leg -> leg.withTimeShift(duration))
      .collect(Collectors.toList());
    var newItin = new Itinerary(timeShiftedLegs);
    newItin.setGeneralizedCost(getGeneralizedCost());
    return newItin;
  }

  /** @see #equals(Object) */
  @Override
  public final int hashCode() {
    return super.hashCode();
  }

  /**
   * Return {@code true} it the other object is the same object using the {@link
   * Object#equals(Object)}. An itinerary is a temporary object and the equals method should not be
   * used for comparision of 2 instances, only to check that to objects are the same instance.
   */
  @Override
  public final boolean equals(Object o) {
    return super.equals(o);
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(Itinerary.class)
      .addStr("from", firstLeg().getFrom().toStringShort())
      .addStr("to", lastLeg().getTo().toStringShort())
      .addTime("start", firstLeg().getStartTime())
      .addTime("end", lastLeg().getEndTime())
      .addNum("nTransfers", numberOfTransfers, -1)
      .addDuration("duration", duration)
      .addNum("generalizedCost", generalizedCost)
      .addDuration("nonTransitTime", nonTransitDuration)
      .addDuration("transitTime", transitDuration)
      .addDuration("waitingTime", waitingDuration)
      .addNum("nonTransitDistance", nonTransitDistanceMeters, "m")
      .addBool("tooSloped", tooSloped)
      .addNum("elevationLost", elevationLost, 0.0)
      .addNum("elevationGained", elevationGained, 0.0)
      .addCol("legs", legs)
      .addObj("fare", fare)
      .toString();
  }

  /**
   * Used to convert an itinerary to a SHORT human readable string - including just a few of the
   * most important fields. It is much shorter and easier to read then the {@link
   * Itinerary#toString()}.
   * <p>
   * It is great for comparing to itineraries in a test: {@code assertEquals(toStr(it1),
   * toStr(it2))}.
   * <p>
   * Example: {@code A ~ Walk 2m ~ B ~ BUS 55 12:04 12:14 ~ C [cost: 1066]}
   * <p>
   * Reads: Start at A, walk 2 minutes to stop B, take bus 55, board at 12:04 and alight at 12:14
   * ...
   */
  public String toStr() {
    // No translater needed, stop indexes are never passed to the builder
    PathStringBuilder buf = new PathStringBuilder(null);
    buf.stop(firstLeg().getFrom().name.toString());

    for (Leg leg : legs) {
      buf.sep();
      if (leg.isWalkingLeg()) {
        buf.walk((int) leg.getDuration().toSeconds());
      } else if (leg instanceof TransitLeg transitLeg) {
        buf.transit(
          transitLeg.getMode().name(),
          transitLeg.getTrip().logName(),
          transitLeg.getStartTime(),
          transitLeg.getEndTime()
        );
      } else if (leg instanceof StreetLeg streetLeg) {
        buf.street(streetLeg.getMode().name(), leg.getStartTime(), leg.getEndTime());
      }

      buf.sep();
      buf.stop(leg.getTo().name.toString());
    }

    buf.space().append(String.format(ROOT, "[ $%d ]", generalizedCost));

    return buf.toString();
  }

  /** Total duration of the itinerary in seconds */
  public Duration getDuration() {
    return duration;
  }

  /**
   * How much time is spent on transit, in seconds.
   */
  public Duration getTransitDuration() {
    return transitDuration;
  }

  /**
   * The number of transfers this trip has.
   */
  public int getNumberOfTransfers() {
    return numberOfTransfers;
  }

  /**
   * How much time is spent waiting for transit to arrive, in seconds.
   */
  public Duration getWaitingDuration() {
    return waitingDuration;
  }

  /**
   * How far the user has to walk, bike and/or drive, in meters.
   */
  public double getNonTransitDistanceMeters() {
    return nonTransitDistanceMeters;
  }

  /** TRUE if mode is WALK from start ot end (all legs are walking). */
  public boolean isWalkOnly() {
    return walkOnly;
  }

  /** TRUE if mode is a non transit move from start ot end (all legs are non-transit). */
  public boolean isStreetOnly() {
    return streetOnly;
  }

  /**
   * System notices is used to tag itineraries with system information. For example if you run the
   * itinerary-filter in debug mode, the filters would tag itineraries instead of deleting them from
   * the result. More than one filter might apply, so there can be more than one notice for an
   * itinerary. This is very handy, when tuning the system or debugging - looking for missing
   * expected trips.
   */
  public List<SystemNotice> getSystemNotices() {
    return List.copyOf(systemNotices);
  }

  /**
   * A list of Legs. Each Leg is either a walking (cycling, car) portion of the trip, or a transit
   * trip on a particular vehicle. So a trip where the use walks to the Q train, transfers to the 6,
   * then walks to their destination, has four legs.
   */
  public List<Leg> getLegs() {
    return legs;
  }

  /**
   * Retrieve a transit leg by its index in the itinerary, starting from 0. This is useful in test
   * where you may assume leg N is a transit leg.
   *
   * @throws ClassCastException if the leg is not a TransitLeg
   */
  public TransitLeg getTransitLeg(int index) {
    return (TransitLeg) legs.get(index);
  }

  /**
   * Retrieve a street leg by its index in the itinerary, starting from 0. This is useful in test
   * where you may assume leg N is a transit leg.
   *
   * @throws ClassCastException if the leg is not a StreetLeg
   */
  public StreetLeg getStreetLeg(int index) {
    return (StreetLeg) legs.get(index);
  }

  public void setLegs(List<Leg> legs) {
    this.legs = List.copyOf(legs);
  }

  /**
   * A sandbox feature for calculating a numeric score between 0 and 1 which indicates how
   * accessible the itinerary is as a whole. This is not a very scientific method but just a rough
   * guidance that expresses certainty or uncertainty about the accessibility.
   * <p>
   * An alternative to this is to use the `generalized-cost` and use that to indicate witch itineraries is the
   * best/most friendly with respect to making the journey in a wheelchair. The `generalized-cost` include, not
   * only a penalty for unknown and inaccessible boardings, but also a penalty for undesired uphill and downhill
   * street traversal.
   * <p>
   * The intended audience for this score are frontend developers wanting to show a simple UI rather
   * than having to iterate over all the stops and trips.
   * <p>
   * Note: the information to calculate this score are all available to the frontend, however
   * calculating them on the backend makes life a little easier and changes are automatically
   * applied to all frontends.
   */
  public Float getAccessibilityScore() {
    return accessibilityScore;
  }

  public void setAccessibilityScore(Float accessibilityScore) {
    this.accessibilityScore = accessibilityScore;
  }

  /**
   * How much time is spent walking/biking/driving, in seconds.
   */
  public Duration getNonTransitDuration() {
    return nonTransitDuration;
  }

  /**
   * How much elevation is lost, in total, over the course of the trip, in meters. As an example, a
   * trip that went from the top of Mount Everest straight down to sea level, then back up K2, then
   * back down again would have an elevationLost of Everest + K2.
   */
  public Double getElevationLost() {
    return elevationLost;
  }

  public void setElevationLost(Double elevationLost) {
    this.elevationLost = DoubleUtils.roundTo2Decimals(elevationLost);
  }

  /**
   * How much elevation is gained, in total, over the course of the trip, in meters. See
   * elevationLost.
   */
  public Double getElevationGained() {
    return elevationGained;
  }

  public void setElevationGained(Double elevationGained) {
    this.elevationGained = DoubleUtils.roundTo2Decimals(elevationGained);
  }

  /**
   * If a generalized cost is used in the routing algorithm, this should be the total cost computed
   * by the algorithm. This is relevant for anyone who want to debug an search and tuning the
   * system. The unit should be equivalent to the cost of "one second of transit".
   * <p>
   * -1 indicate that the cost is not set/computed.
   */
  public int getGeneralizedCost() {
    return generalizedCost;
  }

  public void setGeneralizedCost(int generalizedCost) {
    this.generalizedCost = generalizedCost;
  }

  /**
   * This is the transfer-wait-time-cost. The aim is to distribute wait-time and adding a high
   * penalty on short transfers. Do not use this to compare or filter itineraries. The filtering on
   * this parameter is done on paths, before mapping to itineraries and is provided here as
   * reference information.
   * <p>
   * -1 indicate that the cost is not set/computed.
   */
  public int getWaitTimeOptimizedCost() {
    return waitTimeOptimizedCost;
  }

  public void setWaitTimeOptimizedCost(int waitTimeOptimizedCost) {
    this.waitTimeOptimizedCost = waitTimeOptimizedCost;
  }

  /**
   * This is the transfer-priority-cost. If two paths ride the same trips with different transfers,
   * this cost is used to pick the one with the best transfer constraints (guaranteed, stay-seated,
   * not-allowed ...). Do not use this to compare or filter itineraries. The filtering on this
   * parameter is done on paths, before mapping to itineraries and is provided here as reference
   * information.
   * <p>
   * -1 indicate that the cost is not set/computed.
   */
  public int getTransferPriorityCost() {
    return transferPriorityCost;
  }

  public void setTransferPriorityCost(int transferPriorityCost) {
    this.transferPriorityCost = transferPriorityCost;
  }

  /**
   * This itinerary has a greater slope than the user requested.
   */
  public boolean isTooSloped() {
    return tooSloped;
  }

  public void setTooSloped(boolean tooSloped) {
    this.tooSloped = tooSloped;
  }

  /**
   * The maximum slope for any part of the itinerary.
   */
  public Double getMaxSlope() {
    return maxSlope;
  }

  public void setMaxSlope(Double maxSlope) {
    this.maxSlope = DoubleUtils.roundTo2Decimals(maxSlope);
  }

  /**
   * If {@link RouteRequest#allowArrivingInRentalVehicleAtDestination}
   * is set than it is possible to end a trip without dropping off the rented bicycle.
   */
  public boolean isArrivedAtDestinationWithRentedVehicle() {
    return arrivedAtDestinationWithRentedVehicle;
  }

  public void setArrivedAtDestinationWithRentedVehicle(
    boolean arrivedAtDestinationWithRentedVehicle
  ) {
    this.arrivedAtDestinationWithRentedVehicle = arrivedAtDestinationWithRentedVehicle;
  }

  /**
   * Get the index of a leg when you want to reference it in an API response, for example when you
   * want to say that a fare is valid for legs 2 and 3.
   */
  public int getLegIndex(Leg leg) {
    var index = legs.indexOf(leg);
    // the filter pipeline can also modify the identity of Leg instances. that's why we not only
    // check that but also the start and end point as a replacement for the identity.
    if (index > -1) {
      return index;
    } else {
      for (int i = 0; i < legs.size() - 1; i++) {
        var currentLeg = legs.get(i);
        if (
          currentLeg.getFrom().sameLocation(leg.getFrom()) &&
          currentLeg.getTo().sameLocation(leg.getTo())
        ) {
          return i;
        }
      }
      return -1;
    }
  }

  /**
   * The cost of this trip
   */
  public ItineraryFares getFares() {
    return fare;
  }

  public void setFare(ItineraryFares fare) {
    this.fare = fare;
  }

  public List<ScheduledTransitLeg> getScheduledTransitLegs() {
    return getLegs()
      .stream()
      .filter(ScheduledTransitLeg.class::isInstance)
      .map(ScheduledTransitLeg.class::cast)
      .toList();
  }
}
