package org.opentripplanner.model.plan;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.framework.model.TimeAndCost;
import org.opentripplanner.model.SystemNotice;
import org.opentripplanner.model.fare.ItineraryFares;

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
  ItineraryFares fare = ItineraryFares.empty();

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
    this.elevationLost_m = itinerary.getElevationLost();
    this.elevationGained_m = itinerary.getElevationGained();
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
    this.fare = itinerary.getFares();
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

  public ItineraryBuilder withAccessibilityScore(Float accessibilityScore) {
    this.accessibilityScore = accessibilityScore;
    return this;
  }

  public ItineraryBuilder withEmissionsPerPerson(Emissions emissionsPerPerson) {
    this.emissionsPerPerson = emissionsPerPerson;
    return this;
  }

  public ItineraryBuilder withFare(ItineraryFares fare) {
    this.fare = fare;
    return this;
  }

  public Itinerary build() {
    return new Itinerary(this);
  }
}
