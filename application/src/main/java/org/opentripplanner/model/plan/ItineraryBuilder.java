package org.opentripplanner.model.plan;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.framework.model.TimeAndCost;
import org.opentripplanner.model.SystemNotice;
import org.opentripplanner.model.fare.ItineraryFare;

public class ItineraryBuilder {

  /* GENERAL */
  final boolean searchWindowAware;

  /* COST AND PENALTY */
  TimeAndCost accessPenalty = TimeAndCost.ZERO;
  TimeAndCost egressPenalty = TimeAndCost.ZERO;
  Cost generalizedCost = null;
  Integer generalizedCost2 = null;
  int transferPriorityCost = Itinerary.UNKNOWN;
  int waitTimeOptimizedCost = Itinerary.UNKNOWN;

  /* RENATL */
  boolean arrivedAtDestinationWithRentedVehicle = false;

  /* ELEVATION */
  Double elevationGained_m = 0.0;
  Double elevationLost_m = 0.0;
  Double maxSlope = null;
  boolean tooSloped = false;

  /* LEGS */
  List<Leg> legs;

  /* ITINERARY LIFECYCLE - This is copied over, but not possible to modify in the builder. */
  final List<SystemNotice> systemNotices;

  /* SANDBOX EXPERIMENTAL PROPERTIES */
  Float accessibilityScore;
  Emissions emissionsPerPerson;
  ItineraryFare fare = ItineraryFare.empty();

  ItineraryBuilder(List<Leg> legs, boolean searchWindowAware) {
    this.legs = legs;
    this.searchWindowAware = searchWindowAware;

    // Initialize Itinerary mutable fields, these are not mutable in this builder, but we need to
    // initialize them here for the #build(), Itinerary#init() and Itinerary#copyOf() to work
    // correctly.
    this.systemNotices = new ArrayList<>();
  }

  ItineraryBuilder(Itinerary original) {
    this.legs = original.legs();
    this.searchWindowAware = original.isSearchWindowAware();
    this.accessPenalty = original.accessPenalty();
    this.egressPenalty = original.egressPenalty();
    this.generalizedCost = original.generalizedCostIncludingPenalty();
    this.generalizedCost2 = original.generalizedCost2().orElse(null);
    this.transferPriorityCost = original.transferPriorityCost();
    this.waitTimeOptimizedCost = original.waitTimeOptimizedCost();

    // Elevation
    this.elevationGained_m = original.privateElevationGainedForBuilder();
    this.elevationLost_m = original.privateElevationLostForBuilder();
    this.tooSloped = original.isTooSloped();
    this.maxSlope = original.maxSlope();

    // Flags
    this.arrivedAtDestinationWithRentedVehicle = original.isArrivedAtDestinationWithRentedVehicle();

    // Itinerary Lifecycle - mutable in itinerary, not mutable here
    this.systemNotices = original.privateSystemNoticesForBuilder();

    // Sandbox experimental properties
    this.accessibilityScore = original.accessibilityScore();
    this.emissionsPerPerson = original.emissionsPerPerson();
    this.fare = original.fare();
  }

  /**
   * The total cost of the itinerary including access/egress penalty cost.
   */
  public ItineraryBuilder withGeneralizedCost(Cost generalizedCost) {
    this.generalizedCost = generalizedCost;
    return this;
  }

  /**
   * The the general-cost without access and egress penalty cost.
   */
  Cost calculateGeneralizedCostWithoutPenalty() {
    return generalizedCost.minus(accessPenalty.cost().plus(egressPenalty.cost()));
  }

  public ItineraryBuilder withGeneralizedCost2(Integer generalizedCost2) {
    this.generalizedCost2 = generalizedCost2;
    return this;
  }

  public ItineraryBuilder withTransferPriorityCost(int transferPriorityCost) {
    this.transferPriorityCost = transferPriorityCost;
    return this;
  }

  public ItineraryBuilder withWaitTimeOptimizedCost(int waitTimeOptimizedCost) {
    this.waitTimeOptimizedCost = waitTimeOptimizedCost;
    return this;
  }

  public ItineraryBuilder withAccessPenalty(TimeAndCost accessPenalty) {
    this.accessPenalty = accessPenalty;
    return this;
  }

  public ItineraryBuilder withEgressPenalty(TimeAndCost egressPenalty) {
    this.egressPenalty = egressPenalty;
    return this;
  }

  /**
   * Add the elevation change in meters to the the itinerary summary. The builder will add the
   * change to the {@code elevationGained} or the {@code elevationLost} depending on the sign of
   * the given change value. Negative change is added to the {@code elevationLost} and positive
   * values are added to the {@code elevationGained}.
   *
   * Note! This is in addition to the elevation added for each leg elevation profile. TODO Why?
   */
  public void addElevationChange(double change_m) {
    if (change_m > 0.0) {
      this.elevationGained_m += change_m;
    } else {
      this.elevationLost_m -= change_m;
    }
  }

  public ItineraryBuilder withMaxSlope(double wheelchairPreferencesMaxSlope, double maxSlope) {
    this.maxSlope = maxSlope;
    this.tooSloped = maxSlope > wheelchairPreferencesMaxSlope;
    return this;
  }

  public ItineraryBuilder withArrivedAtDestinationWithRentedVehicle(
    boolean arrivedAtDestinationWithRentedVehicle
  ) {
    this.arrivedAtDestinationWithRentedVehicle = arrivedAtDestinationWithRentedVehicle;
    return this;
  }

  /**
   * Decorate the existing legs with new information. This method takes a lambda to make sure
   * the caller uses the right set of legs as abase for the decoration, and not just set a
   * new set of legs - witch may lead to information loss.
   */
  public ItineraryBuilder withLegs(Function<List<Leg>, List<Leg>> body) {
    this.legs = body.apply(legs);
    return this;
  }

  public List<Leg> legs() {
    return legs;
  }

  /**
   * Applies the transformation in {@code mapper} to all instances of {@link TransitLeg} in the
   * legs of this Itinerary.
   */
  public ItineraryBuilder transformTransitLegs(Function<TransitLeg, TransitLeg> mapper) {
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
    return this;
  }

  public ItineraryBuilder withAccessibilityScore(Float accessibilityScore) {
    this.accessibilityScore = accessibilityScore;
    return this;
  }

  public ItineraryBuilder withEmissionsPerPerson(Emissions emissionsPerPerson) {
    this.emissionsPerPerson = emissionsPerPerson;
    return this;
  }

  public ItineraryFare fare() {
    return fare;
  }

  public ItineraryBuilder withFare(ItineraryFare fare) {
    this.fare = fare;
    return this;
  }

  public Itinerary build() {
    return new Itinerary(this);
  }
}
