package org.opentripplanner.model.plan;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.opentripplanner.ext.flex.FlexibleTransitLeg;
import org.opentripplanner.framework.model.TimeAndCost;
import org.opentripplanner.model.SystemNotice;
import org.opentripplanner.model.fare.ItineraryFares;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.raptor.api.model.RaptorCostConverter;
import org.opentripplanner.raptor.api.path.PathStringBuilder;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.preference.ItineraryFilterPreferences;
import org.opentripplanner.utils.lang.DoubleUtils;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * An Itinerary is one complete way of getting from the start location to the end location.
 */
public class Itinerary implements ItinerarySortKey {

  public static final int UNKNOWN = -1;

  /* final primitive properties */
  private final Duration duration;
  private final Duration transitDuration;
  private final int numberOfTransfers;
  private final Duration waitingDuration;
  private final double nonTransitDistanceMeters;
  private final boolean walkOnly;
  private final boolean streetOnly;
  private final Duration nonTransitDuration;
  private final Duration walkDuration;
  private final double walkDistanceMeters;

  /* mutable primitive properties */
  private Double elevationLost = 0.0;
  private Double elevationGained = 0.0;
  private int generalizedCost = UNKNOWN;
  private Integer generalizedCost2 = null;
  private TimeAndCost accessPenalty = TimeAndCost.ZERO;
  private TimeAndCost egressPenalty = TimeAndCost.ZERO;
  private int waitTimeOptimizedCost = UNKNOWN;
  private int transferPriorityCost = UNKNOWN;
  private boolean tooSloped = false;
  private Double maxSlope = null;
  private boolean arrivedAtDestinationWithRentedVehicle = false;

  /* Sandbox experimental properties */
  private Float accessibilityScore;

  private Emissions emissionsPerPerson;

  /* other properties */

  private final List<SystemNotice> systemNotices = new ArrayList<>();
  private final boolean searchWindowAware;
  private List<Leg> legs;

  private ItineraryFares fare = ItineraryFares.empty();

  private Itinerary(List<Leg> legs, boolean searchWindowAware) {
    setLegs(legs);
    this.searchWindowAware = searchWindowAware;

    // Set aggregated data
    ItinerariesCalculateLegTotals totals = new ItinerariesCalculateLegTotals(legs);
    this.duration = totals.totalDuration;
    this.numberOfTransfers = totals.transfers();
    this.transitDuration = totals.transitDuration;
    this.nonTransitDuration = totals.nonTransitDuration;
    this.nonTransitDistanceMeters = DoubleUtils.roundTo2Decimals(totals.nonTransitDistanceMeters);
    this.walkDuration = totals.walkDuration;
    this.walkDistanceMeters = totals.walkDistanceMeters;
    this.waitingDuration = totals.waitingDuration;
    this.walkOnly = totals.walkOnly;
    this.streetOnly = totals.streetOnly;
    this.setElevationGained(totals.totalElevationGained);
    this.setElevationLost(totals.totalElevationLost);
  }

  /**
   * Creates an itinerary that contains scheduled transit which is aware of the search window.
   */
  public static Itinerary createScheduledTransitItinerary(List<Leg> legs) {
    return new Itinerary(legs, true);
  }

  /**
   * Creates an itinerary that creates only street or flex results which are not aware of the
   * time window.
   */
  public static Itinerary createDirectItinerary(List<Leg> legs) {
    return new Itinerary(legs, false);
  }

  /**
   * Time that the trip departs.
   */
  public ZonedDateTime startTime() {
    return firstLeg().getStartTime();
  }

  /**
   * Time that the trip departs as a Java Instant type.
   */
  public Instant startTimeAsInstant() {
    return firstLeg().getStartTime().toInstant();
  }

  /**
   * Time that the trip arrives.
   */
  public ZonedDateTime endTime() {
    return lastLeg().getEndTime();
  }

  /**
   * Time that the trip arrives as a Java Instant type.
   */
  public Instant endTimeAsInstant() {
    return lastLeg().getEndTime().toInstant();
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
    return getLegs()
      .stream()
      // An unknown distance is -1
      .filter(l -> l.getDistanceMeters() > 0)
      .mapToDouble(Leg::getDistanceMeters)
      .sum();
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

  /**
   * Returns true if this itinerary has only flex and walking legs.
   */
  public boolean isDirectFlex() {
    var containsFlex = legs.stream().anyMatch(Leg::isFlexibleTrip);
    var flexOrWalkOnly = legs.stream().allMatch(l -> l.isFlexibleTrip() || l.isWalkingLeg());
    return containsFlex && flexOrWalkOnly;
  }

  /** TRUE if at least one leg is a transit leg. */
  public boolean hasTransit() {
    return legs
      .stream()
      .anyMatch(l -> l instanceof ScheduledTransitLeg || l instanceof FlexibleTransitLeg);
  }

  /**
   * Returns true if this itinerary was produced by an algorithm that is aware of the search window.
   * As of 2024 only the itineraries produced by RAPTOR that do that.
   */
  public boolean isSearchWindowAware() {
    return searchWindowAware;
  }

  public Leg firstLeg() {
    return getLegs().get(0);
  }

  public Leg lastLeg() {
    return getLegs().get(getLegs().size() - 1);
  }

  /**
   * An itinerary can be flagged for removal with a system notice.
   * <p>
   * For example when tuning or manually testing the itinerary-filter-chain it you can enable
   * {@link ItineraryFilterPreferences#debug()} and instead of removing itineraries from the result
   * the itineraries will be tagged by the filters instead. This enables investigating, why an
   * expected itinerary is missing from the result set. It can be also used by other filters to see
   * the already filtered itineraries.
   */
  public void flagForDeletion(SystemNotice notice) {
    systemNotices.add(notice);
  }

  /**
   * Remove all deletion flags of this itinerary, in effect undeleting it from the result.
   */
  public void removeDeletionFlags(Set<String> removeTags) {
    systemNotices.removeIf(it -> removeTags.contains(it.tag()));
  }

  public boolean isFlaggedForDeletion() {
    return !systemNotices.isEmpty();
  }

  /**
   * Utility method to check if one of the attached system notices matches the
   * given {@code tag}.
   */
  public boolean hasSystemNoticeTag(String tag) {
    return systemNotices.stream().map(SystemNotice::tag).anyMatch(tag::equals);
  }

  public Itinerary withTimeShiftToStartAt(ZonedDateTime afterTime) {
    Duration duration = Duration.between(firstLeg().getStartTime(), afterTime);
    List<Leg> timeShiftedLegs = getLegs()
      .stream()
      .map(leg -> leg.withTimeShift(duration))
      .collect(Collectors.toList());
    var newItin = new Itinerary(timeShiftedLegs, searchWindowAware);
    newItin.setGeneralizedCost(getGeneralizedCost());
    return newItin;
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
   * Applies the transformation in {@code mapper} to all instances of {@link TransitLeg} in the
   * legs of this Itinerary.
   * <p>
   * NOTE: The itinerary is mutable so the transformation is done in-place!
   */
  public void transformTransitLegs(Function<TransitLeg, TransitLeg> mapper) {
    legs = legs
      .stream()
      .map(l -> {
        if (l instanceof TransitLeg tl) {
          return mapper.apply(tl);
        } else {
          return l;
        }
      })
      .toList();
  }

  public Stream<StreetLeg> getStreetLegs() {
    return legs.stream().filter(StreetLeg.class::isInstance).map(StreetLeg.class::cast);
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
   * An alternative to this is to use the `generalized-cost` and use that to indicate which itineraries is the
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
   * by the algorithm. This is relevant for anyone who want to debug a search and tuning the
   * system. The unit should be equivalent to the cost of "one second of transit".
   * <p>
   * -1 indicate that the cost is not set/computed.
   */
  public int getGeneralizedCost() {
    return generalizedCost;
  }

  /**
   * If a generalized cost is used in the routing algorithm, this is the cost computed plus
   * the artificial penalty added for access/egresses. This is useful so that itineraries
   * using only on-street legs don't have an unfair advantage over those combining access/egress with
   * transit and using a penalty when being processed by the itinerary filter chain.
   *
   * @see org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressPenaltyDecorator
   */
  @Override
  public int getGeneralizedCostIncludingPenalty() {
    return generalizedCost + penaltyCost(accessPenalty) + penaltyCost(egressPenalty);
  }

  public void setGeneralizedCost(int generalizedCost) {
    this.generalizedCost = generalizedCost;
  }

  /**
   * The transit router allows the usage of a second generalized-cost parameter to be used in
   * routing. In Raptor this is called c2, but in OTP it is generalized-cost-2. What this cost
   * represents depends on the use-case and the unit and scale is also given by the use-case.
   * <p>
   * Currently, the pass-through search and the transit-priority uses this. This is relevant for
   * anyone who wants to debug a search and tune the system.
   * <p>
   * {@link RaptorConstants#NOT_SET} indicate that the cost is not set/computed.
   */
  public Optional<Integer> getGeneralizedCost2() {
    return Optional.ofNullable(generalizedCost2);
  }

  public void setGeneralizedCost2(Integer generalizedCost2) {
    this.generalizedCost2 = generalizedCost2;
  }

  @Nullable
  public TimeAndCost getAccessPenalty() {
    return accessPenalty;
  }

  public void setAccessPenalty(TimeAndCost accessPenalty) {
    Objects.requireNonNull(accessPenalty);
    this.accessPenalty = accessPenalty;
  }

  @Nullable
  public TimeAndCost getEgressPenalty() {
    return egressPenalty;
  }

  public void setEgressPenalty(TimeAndCost egressPenalty) {
    Objects.requireNonNull(egressPenalty);
    this.egressPenalty = egressPenalty;
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
   * <p>
   * Return {@link #UNKNOWN} if not found.
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
      return UNKNOWN;
    }
  }

  /**
   * The fare products of this itinerary.
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

  /**
   * The emissions of this itinerary.
   */
  public void setEmissionsPerPerson(Emissions emissionsPerPerson) {
    this.emissionsPerPerson = emissionsPerPerson;
  }

  @Nullable
  public Emissions getEmissionsPerPerson() {
    return this.emissionsPerPerson;
  }

  /**
   * How much walking this itinerary contains, in meters.
   */
  public double walkDistanceMeters() {
    return walkDistanceMeters;
  }

  /**
   * How long the walking is contained in this itinerary.
   */
  public Duration walkDuration() {
    return walkDuration;
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
    return ToStringBuilder.of(Itinerary.class)
      .addStr("from", firstLeg().getFrom().toStringShort())
      .addStr("to", lastLeg().getTo().toStringShort())
      .addTime("start", firstLeg().getStartTime())
      .addTime("end", lastLeg().getEndTime())
      .addNum("nTransfers", numberOfTransfers)
      .addDuration("duration", duration)
      .addDuration("nonTransitTime", nonTransitDuration)
      .addDuration("transitTime", transitDuration)
      .addDuration("waitingTime", waitingDuration)
      .addNum("generalizedCost", generalizedCost, UNKNOWN)
      .addNum("generalizedCost2", generalizedCost2)
      .addNum("waitTimeOptimizedCost", waitTimeOptimizedCost, UNKNOWN)
      .addNum("transferPriorityCost", transferPriorityCost, UNKNOWN)
      .addNum("nonTransitDistance", nonTransitDistanceMeters, "m")
      .addBool("tooSloped", tooSloped)
      .addNum("elevationLost", elevationLost, 0.0)
      .addNum("elevationGained", elevationGained, 0.0)
      .addCol("legs", legs)
      .addObj("fare", fare)
      .addObj("emissionsPerPerson", emissionsPerPerson)
      .toString();
  }

  /**
   * Used to convert a list of itineraries to a SHORT human-readable string.
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
      buf.stop(leg.getTo().name.toString());
    }

    // The generalizedCost2 is printed as is, it is a special cost and the scale depends on the
    // use-case.
    buf.summary(
      RaptorCostConverter.toRaptorCost(generalizedCost),
      getGeneralizedCost2().orElse(RaptorConstants.NOT_SET)
    );

    return buf.toString();
  }

  private static int penaltyCost(TimeAndCost penalty) {
    return penalty.cost().toSeconds();
  }
}
