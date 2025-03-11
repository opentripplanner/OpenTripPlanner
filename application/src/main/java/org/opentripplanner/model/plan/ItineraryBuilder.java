package org.opentripplanner.model.plan;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.framework.model.TimeAndCost;
import org.opentripplanner.model.SystemNotice;

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
  Double elevationGained = 0.0;
  Double elevationLost = 0.0;
  Double maxSlope = null;
  boolean tooSloped = false;

  /* LEGS */
  List<Leg> legs;

  /* ITINERARY LIFECYCLE - This is copied over, but not possible to modify in the builder. */
  final List<SystemNotice> systemNotices;

  /* SANDBOX EXPERIMENTAL PROPERTIES */
  Float accessibilityScore;
  Emissions emissionsPerPerson;

  ItineraryBuilder(List<Leg> legs, boolean searchWindowAware) {
    this.legs = legs;
    this.searchWindowAware = searchWindowAware;

    // Initialize Itinerary mutable fields, these are not mutable in this builder, but we need to
    // initialize them here for the #build(), Itinerary#init() and Itinerary#copyOf() to work
    // correctly.
    this.systemNotices = new ArrayList<>();
  }

  ItineraryBuilder(Itinerary itinerary) {
    this.legs = itinerary.getLegs();
    this.searchWindowAware = itinerary.isSearchWindowAware();
    this.accessPenalty = itinerary.getAccessPenalty();
    this.egressPenalty = itinerary.getEgressPenalty();
    this.generalizedCost = Cost.costOfSeconds(itinerary.getGeneralizedCostIncludingPenalty());
    this.generalizedCost2 = itinerary.getGeneralizedCost2().orElse(null);
    this.transferPriorityCost = itinerary.getTransferPriorityCost();
    this.waitTimeOptimizedCost = itinerary.getWaitTimeOptimizedCost();

    // Elevation
    this.elevationLost = itinerary.getElevationLost();
    this.elevationGained = itinerary.getElevationGained();
    this.tooSloped = itinerary.isTooSloped();
    this.maxSlope = itinerary.getMaxSlope();

    // Flags
    this.arrivedAtDestinationWithRentedVehicle =
      itinerary.isArrivedAtDestinationWithRentedVehicle();

    // Itinerary Lifecycle - mutable in itinerary, not mutable here
    this.systemNotices = itinerary.privateSystemNoticesForBuilder();

    // Sandbox experimental properties
    this.accessibilityScore = itinerary.getAccessibilityScore();
    this.emissionsPerPerson = itinerary.getEmissionsPerPerson();
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

  public ItineraryBuilder withElevationLost(Double elevationLost) {
    this.elevationLost = elevationLost;
    return this;
  }

  public ItineraryBuilder withElevationGained(Double elevationGained) {
    this.elevationGained = elevationGained;
    return this;
  }

  public ItineraryBuilder withTooSloped(boolean tooSloped) {
    this.tooSloped = tooSloped;
    return this;
  }

  public ItineraryBuilder withMaxSlope(Double maxSlope) {
    this.maxSlope = maxSlope;
    return this;
  }

  public ItineraryBuilder withArrivedAtDestinationWithRentedVehicle(
    boolean arrivedAtDestinationWithRentedVehicle
  ) {
    this.arrivedAtDestinationWithRentedVehicle = arrivedAtDestinationWithRentedVehicle;
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

  public Itinerary build() {
    return new Itinerary(this);
  }
}
