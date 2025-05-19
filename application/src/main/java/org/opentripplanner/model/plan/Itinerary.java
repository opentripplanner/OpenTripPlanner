package org.opentripplanner.model.plan;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.opentripplanner.ext.flex.FlexibleTransitLeg;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.framework.model.TimeAndCost;
import org.opentripplanner.model.SystemNotice;
import org.opentripplanner.model.fare.ItineraryFare;
import org.opentripplanner.model.plan.leg.ScheduledTransitLeg;
import org.opentripplanner.model.plan.leg.StreetLeg;
import org.opentripplanner.raptor.api.model.RaptorConstants;
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

  /* GENERAL */
  private final Duration totalDuration;
  private final boolean searchWindowAware;

  /* COST AND PENALTY */
  private final TimeAndCost accessPenalty;
  private final TimeAndCost egressPenalty;
  private final Cost generalizedCost;

  @Nullable
  private final Integer generalizedCost2;

  private final int transferPriorityCost;
  private final int waitTimeOptimizedCost;

  /* TRANSIT */
  private final int numberOfTransfers;
  private final Duration totalTransitDuration;

  /* STREET AND WALK */
  private final double totalStreetDistanceMeters;
  private final Duration totalStreetDuration;
  private final boolean streetOnly;
  private final double totalWalkDistanceMeters;
  private final Duration totalWalkDuration;
  private final boolean walkOnly;

  /* RENATL */
  private final boolean arrivedAtDestinationWithRentedVehicle;

  /* WAIT */
  private final Duration totalWaitingDuration;

  /* ELEVATION */
  // TODO See #elevationGained()
  private final double elevationGained_edges_m;
  private final double totalElevationGained_m;
  private final double elevationLost_edges_m;
  private final double totalElevationLost_m;
  private final Double maxSlope;
  private final boolean tooSloped;

  /* LEGS */
  private final List<Leg> legs;

  /* ITINERARY LIFECYCLE - MUTABLE FIELDS */

  /**
   * The systemNotices is part of the itinerary "life-cycle" and is intended to be
   * MUTABLE. We add new system-notices as part of the itinerary filter chain.
   */
  private final List<SystemNotice> systemNotices;

  /* SANDBOX EXPERIMENTAL PROPERTIES */

  private final Float accessibilityScore;
  private final Emission emissionPerPerson;
  private final ItineraryFare fare;

  Itinerary(ItineraryBuilder builder) {
    this.legs = List.copyOf(builder.legs);

    this.generalizedCost = Objects.requireNonNull(builder.calculateGeneralizedCostWithoutPenalty());
    this.generalizedCost2 = builder.generalizedCost2;

    this.searchWindowAware = builder.searchWindowAware;
    this.transferPriorityCost = builder.transferPriorityCost;
    this.waitTimeOptimizedCost = builder.waitTimeOptimizedCost;
    this.accessPenalty = Objects.requireNonNull(builder.accessPenalty);
    this.egressPenalty = Objects.requireNonNull(builder.egressPenalty);
    this.tooSloped = builder.tooSloped;
    this.maxSlope = builder.maxSlope;
    this.elevationGained_edges_m = builder.elevationGained_m;
    this.elevationLost_edges_m = builder.elevationLost_m;
    this.arrivedAtDestinationWithRentedVehicle = builder.arrivedAtDestinationWithRentedVehicle;
    this.systemNotices = builder.systemNotices;
    this.accessibilityScore = builder.accessibilityScore;
    this.emissionPerPerson = builder.emissionPerPerson;
    this.fare = builder.fare;

    // Set aggregated data
    ItinerariesCalculateLegTotals totals = new ItinerariesCalculateLegTotals(legs);

    this.totalDuration = totals.totalDuration;
    this.streetOnly = totals.streetOnly;
    this.numberOfTransfers = totals.transfers();
    this.totalTransitDuration = totals.transitDuration;
    this.totalStreetDuration = totals.onStreetDuration;
    this.totalStreetDistanceMeters = DoubleUtils.roundTo2Decimals(totals.onStreetDistanceMeters);
    this.totalWalkDuration = totals.walkDuration;
    this.totalWalkDistanceMeters = totals.walkDistanceMeters;
    this.walkOnly = totals.walkOnly;
    this.totalWaitingDuration = totals.waitingDuration;
    this.totalElevationGained_m = totals.elevationGained_m;
    this.totalElevationLost_m = totals.elevationLost_m;
  }

  /**
   * Creates an itinerary that contains scheduled transit which is aware of the search window.
   */
  public static ItineraryBuilder ofScheduledTransit(List<Leg> legs) {
    return new ItineraryBuilder(legs, true);
  }

  /**
   * Creates an itinerary that creates only street or flex results which are not aware of the
   * input request time-window.
   */
  public static ItineraryBuilder ofDirect(List<Leg> legs) {
    return new ItineraryBuilder(legs, false);
  }

  public ItineraryBuilder copyOf() {
    return new ItineraryBuilder(this);
  }

  /**
   * Time that the trip departs.
   */
  public ZonedDateTime startTime() {
    return legs().getFirst().startTime();
  }

  /**
   * Time that the trip departs as a Java Instant type.
   */
  public Instant startTimeAsInstant() {
    return legs().getFirst().startTime().toInstant();
  }

  /**
   * Time that the trip arrives.
   */
  public ZonedDateTime endTime() {
    return legs().getLast().endTime();
  }

  /**
   * Time that the trip arrives as a Java Instant type.
   */
  public Instant endTimeAsInstant() {
    return legs().getLast().endTime().toInstant();
  }

  /**
   * Reflects the departureDelay on the first Leg Unit: seconds.
   */
  public int departureDelay() {
    return legs().getFirst().departureDelay();
  }

  /**
   * Reflects the arrivalDelay on the last Leg Unit: seconds.
   */
  public int arrivalDelay() {
    return legs().getLast().arrivalDelay();
  }

  /**
   * This is the amount of time used to travel. {@code waitingTime} is NOT included.
   */
  public Duration effectiveDuration() {
    return totalTransitDuration().plus(totalStreetDuration());
  }

  /**
   * Total distance in meters.
   */
  public double distanceMeters() {
    return legs()
      .stream()
      // An unknown distance is -1
      .filter(l -> l.distanceMeters() > 0)
      .mapToDouble(Leg::distanceMeters)
      .sum();
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
    // TODO This does not look correct, replace with !streetLeg
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
    Duration duration = Duration.between(legs().getFirst().startTime(), afterTime);
    List<Leg> timeShiftedLegs = legs()
      .stream()
      .map(leg -> leg.withTimeShift(duration))
      .collect(Collectors.toList());
    return new ItineraryBuilder(timeShiftedLegs, searchWindowAware)
      .withGeneralizedCost(generalizedCost)
      .withAccessPenalty(accessPenalty)
      .withEgressPenalty(egressPenalty)
      .build();
  }

  /** Total duration of the itinerary in seconds */
  public Duration totalDuration() {
    return totalDuration;
  }

  /**
   * How much time is spent on transit, in seconds.
   */
  public Duration totalTransitDuration() {
    return totalTransitDuration;
  }

  /**
   * The number of transfers this trip has.
   */
  public int numberOfTransfers() {
    return numberOfTransfers;
  }

  /**
   * How much time is spent waiting for transit to arrive, in seconds.
   */
  public Duration totalWaitingDuration() {
    return totalWaitingDuration;
  }

  /**
   * How far the user has to walk, bike and/or drive, in meters.
   */
  public double totalStreetDistanceMeters() {
    return totalStreetDistanceMeters;
  }

  /** TRUE if mode is WALK from start ot end (all legs are walking). */
  public boolean isWalkOnly() {
    return walkOnly;
  }

  /** TRUE if mode is a non transit move from start ot end (all legs are non-transit). */
  @Override
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
  public List<SystemNotice> systemNotices() {
    return List.copyOf(systemNotices);
  }

  List<SystemNotice> privateSystemNoticesForBuilder() {
    return systemNotices;
  }

  /**
   * A list of Legs. Each Leg is either a walking (cycling, car) portion of the trip, or a transit
   * trip on a particular vehicle. So a trip where the use walks to the Q train, transfers to the 6,
   * then walks to their destination, has four legs.
   */
  public List<Leg> legs() {
    return legs;
  }

  public Stream<StreetLeg> streetLegs() {
    return legs.stream().filter(StreetLeg.class::isInstance).map(StreetLeg.class::cast);
  }

  /**
   * Retrieve a transit leg by its index in the itinerary, starting from 0. This is useful in test
   * where you may assume leg N is a transit leg.
   *
   * @throws ClassCastException if the leg is not a TransitLeg
   */
  public TransitLeg transitLeg(int index) {
    return (TransitLeg) legs.get(index);
  }

  /**
   * Retrieve a street leg by its index in the itinerary, starting from 0. This is useful in test
   * where you may assume leg N is a transit leg.
   *
   * @throws ClassCastException if the leg is not a StreetLeg
   */
  public StreetLeg streetLeg(int index) {
    return (StreetLeg) legs.get(index);
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
  public Float accessibilityScore() {
    return accessibilityScore;
  }

  /**
   * How much time is spent walking/biking/driving, in seconds.
   */
  public Duration totalStreetDuration() {
    return totalStreetDuration;
  }

  /**
   * How much elevation is gained, in total, over the course of the trip, in meters. For more info,
   * see {@link #totalElevationLost()}.
   */
  public Double totalElevationGained() {
    // TODO - Why is the elevation computed in two places and added together - this at least needs
    //        to be documented. This is also a source for error, since 'elevationGained_edges_m'
    //        is computed onece, while the 'elevationGained_total_m' is computed every time the
    //        legs changes. If street-legs are replaced then only the calculation of the that
    //        is done in the total calculation is correct. We keep the 'elevationGained_edges_m'
    //        around, since it is "probably" better to include it when the legs are changed instead
    //        of dropping it. There is an error here, for example the DecorateConsolidatedStopNames
    //        does not work with this.
    return DoubleUtils.roundTo2Decimals(elevationGained_edges_m + totalElevationGained_m);
  }

  double privateElevationGainedForBuilder() {
    return elevationGained_edges_m;
  }

  /**
   * How much elevation is lost, in total, over the course of the trip, in meters. As an example, a
   * trip that went from the top of Mount Everest straight down to sea level, then back up K2, then
   * back down again would have an elevationLost of Everest + K2.
   */
  public Double totalElevationLost() {
    // TODO - See #getElevationGained().
    return DoubleUtils.roundTo2Decimals(elevationLost_edges_m + totalElevationLost_m);
  }

  double privateElevationLostForBuilder() {
    return elevationLost_edges_m;
  }

  /**
   * If a generalized cost is used in the routing algorithm, this should be the total cost computed
   * by the algorithm. This is relevant for anyone who want to debug a search and tuning the
   * system. The unit should be equivalent to the cost of "one second of transit".
   * <p>
   * Zero(0) cost indicate that the cost is not set/computed.
   */
  public int generalizedCost() {
    // TODO Return Cost type, not int
    return generalizedCost.toSeconds();
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
  public Cost generalizedCostIncludingPenalty() {
    return generalizedCost.plus(accessPenalty.cost().plus(egressPenalty.cost()));
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
  public Optional<Integer> generalizedCost2() {
    return Optional.ofNullable(generalizedCost2);
  }

  @Nullable
  public TimeAndCost accessPenalty() {
    return accessPenalty;
  }

  @Nullable
  public TimeAndCost egressPenalty() {
    return egressPenalty;
  }

  /**
   * This is the transfer-wait-time-cost. The aim is to distribute wait-time and adding a high
   * penalty on short transfers. Do not use this to compare or filter itineraries. The filtering on
   * this parameter is done on paths, before mapping to itineraries and is provided here as
   * reference information.
   * <p>
   * -1 indicate that the cost is not set/computed.
   */
  public int waitTimeOptimizedCost() {
    return waitTimeOptimizedCost;
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
  public int transferPriorityCost() {
    return transferPriorityCost;
  }

  /**
   * This itinerary has a greater slope than the user requested.
   */
  public boolean isTooSloped() {
    return tooSloped;
  }

  /**
   * The maximum slope for any part of the itinerary.
   * TODO Document unit
   */
  public Double maxSlope() {
    return maxSlope;
  }

  /**
   * If {@link RouteRequest#allowArrivingInRentalVehicleAtDestination}
   * is set than it is possible to end a trip without dropping off the rented bicycle.
   */
  public boolean isArrivedAtDestinationWithRentedVehicle() {
    return arrivedAtDestinationWithRentedVehicle;
  }

  /**
   * Get the index of a leg when you want to reference it in an API response, for example when you
   * want to say that a fare is valid for legs 2 and 3.
   * <p>
   * Return {@link #UNKNOWN} if not found.
   */
  public int findLegIndex(Leg leg) {
    var index = legs.indexOf(leg);
    // the filter pipeline can also modify the identity of Leg instances. that's why we not only
    // check that but also the start and end point as a replacement for the identity.
    if (index > -1) {
      return index;
    } else {
      for (int i = 0; i < legs.size() - 1; i++) {
        var currentLeg = legs.get(i);
        if (currentLeg.from().sameLocation(leg.from()) && currentLeg.to().sameLocation(leg.to())) {
          return i;
        }
      }
      return UNKNOWN;
    }
  }

  public List<ScheduledTransitLeg> listScheduledTransitLegs() {
    return legs()
      .stream()
      .filter(ScheduledTransitLeg.class::isInstance)
      .map(ScheduledTransitLeg.class::cast)
      .toList();
  }

  @Nullable
  public Emission emissionPerPerson() {
    return this.emissionPerPerson;
  }

  /**
   * How much walking this itinerary contains, in meters.
   */
  public double totalWalkDistanceMeters() {
    return totalWalkDistanceMeters;
  }

  /**
   * How long the walking is contained in this itinerary.
   */
  public Duration totalWalkDuration() {
    return totalWalkDuration;
  }

  /**
   * The fare products of this itinerary.
   */
  public ItineraryFare fare() {
    return fare;
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
      .addStr("from", legs().getFirst().from().toStringShort())
      .addStr("to", legs().getLast().to().toStringShort())
      .addTime("start", legs().getFirst().startTime())
      .addTime("end", legs().getLast().endTime())
      .addNum("nTransfers", numberOfTransfers)
      .addDuration("duration", totalDuration)
      .addDuration("nonTransitTime", totalStreetDuration)
      .addDuration("transitTime", totalTransitDuration)
      .addDuration("waitingTime", totalWaitingDuration)
      .addObj("generalizedCost", generalizedCost)
      .addNum("generalizedCost2", generalizedCost2)
      .addNum("waitTimeOptimizedCost", waitTimeOptimizedCost, UNKNOWN)
      .addNum("transferPriorityCost", transferPriorityCost, UNKNOWN)
      .addNum("nonTransitDistance", totalStreetDistanceMeters, "m")
      .addBool("tooSloped", tooSloped)
      .addNum("elevationGained", totalElevationGained(), "m")
      .addNum("elevationLost", totalElevationLost(), "m")
      .addCol("legs", legs)
      .addObj("emissionPerPerson", emissionPerPerson)
      .addObj("fare", fare)
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
    buf.stop(legs().getFirst().from().name.toString());

    for (Leg leg : legs) {
      if (leg.isWalkingLeg()) {
        buf.walk((int) leg.duration().toSeconds());
      } else if (leg instanceof TransitLeg transitLeg) {
        buf.transit(
          transitLeg.mode().name(),
          transitLeg.trip().logName(),
          transitLeg.startTime(),
          transitLeg.endTime()
        );
      } else if (leg instanceof StreetLeg streetLeg) {
        buf.street(streetLeg.getMode().name(), leg.startTime(), leg.endTime());
      }
      buf.stop(leg.to().name.toString());
    }

    // The generalizedCost2 is printed as is, it is a special cost and the scale depends on the
    // use-case.
    buf.summary(
      generalizedCost.toCentiSeconds(),
      generalizedCost2().orElse(RaptorConstants.NOT_SET)
    );

    return buf.toString();
  }
}
