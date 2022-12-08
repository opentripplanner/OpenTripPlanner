package org.opentripplanner.routing.api.request.preference;

import java.util.Objects;
import java.util.function.Consumer;
import org.opentripplanner.routing.algorithm.filterchain.api.TransitGeneralizedCostFilterParams;
import org.opentripplanner.routing.api.request.framework.DoubleAlgorithmFunction;
import org.opentripplanner.routing.api.request.framework.RequestFunctions;
import org.opentripplanner.routing.api.request.framework.Units;

/**
 * Group by Similarity filter parameters. See the configuration for documentation of each field.
 *
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
 */
public final class ItineraryFilterPreferences {

  public static final ItineraryFilterPreferences DEFAULT = new ItineraryFilterPreferences();

  private final boolean debug;
  private final double groupSimilarityKeepOne;
  private final double groupSimilarityKeepThree;
  private final double groupedOtherThanSameLegsMaxCostMultiplier;
  private final TransitGeneralizedCostFilterParams transitGeneralizedCostLimit;
  private final double bikeRentalDistanceRatio;
  private final double parkAndRideDurationRatio;
  private final DoubleAlgorithmFunction nonTransitGeneralizedCostLimit;
  private final boolean filterItinerariesWithSameFirstOrLastTrip;
  private final boolean accessibilityScore;
  private final boolean removeItinerariesWithSameRoutesAndStops;
  private final double minBikeParkingDistance;

  private ItineraryFilterPreferences() {
    this.debug = false;
    this.groupSimilarityKeepOne = 0.85;
    this.groupSimilarityKeepThree = 0.68;
    this.groupedOtherThanSameLegsMaxCostMultiplier = 2.0;
    this.bikeRentalDistanceRatio = 0.0;
    this.parkAndRideDurationRatio = 0.0;
    this.transitGeneralizedCostLimit =
      new TransitGeneralizedCostFilterParams(RequestFunctions.createLinearFunction(900, 1.5), 0.4);
    this.nonTransitGeneralizedCostLimit = RequestFunctions.createLinearFunction(3600, 2);
    this.filterItinerariesWithSameFirstOrLastTrip = false;
    this.accessibilityScore = false;
    this.removeItinerariesWithSameRoutesAndStops = false;
    this.minBikeParkingDistance = 0;
  }

  private ItineraryFilterPreferences(Builder builder) {
    this.debug = builder.debug;
    this.groupSimilarityKeepOne = Units.reluctance(builder.groupSimilarityKeepOne);
    this.groupSimilarityKeepThree = Units.reluctance(builder.groupSimilarityKeepThree);
    this.groupedOtherThanSameLegsMaxCostMultiplier =
      Units.reluctance(builder.groupedOtherThanSameLegsMaxCostMultiplier);
    this.transitGeneralizedCostLimit = Objects.requireNonNull(builder.transitGeneralizedCostLimit);
    this.nonTransitGeneralizedCostLimit =
      Objects.requireNonNull(builder.nonTransitGeneralizedCostLimit);
    this.bikeRentalDistanceRatio = Units.ratio(builder.bikeRentalDistanceRatio);
    this.parkAndRideDurationRatio = Units.ratio(builder.parkAndRideDurationRatio);
    this.filterItinerariesWithSameFirstOrLastTrip =
      builder.filterItinerariesWithSameFirstOrLastTrip;
    this.accessibilityScore = builder.accessibilityScore;
    this.removeItinerariesWithSameRoutesAndStops = builder.removeItinerariesWithSameRoutesAndStops;
    this.minBikeParkingDistance = builder.minBikeParkingDistance;
  }

  public static Builder of() {
    return DEFAULT.copyOf();
  }

  public Builder copyOf() {
    return new Builder(this);
  }

  public boolean debug() {
    return debug;
  }

  public double groupSimilarityKeepOne() {
    return groupSimilarityKeepOne;
  }

  public double groupSimilarityKeepThree() {
    return groupSimilarityKeepThree;
  }

  public double groupedOtherThanSameLegsMaxCostMultiplier() {
    return groupedOtherThanSameLegsMaxCostMultiplier;
  }

  public TransitGeneralizedCostFilterParams transitGeneralizedCostLimit() {
    return transitGeneralizedCostLimit;
  }

  public double bikeRentalDistanceRatio() {
    return bikeRentalDistanceRatio;
  }

  public double parkAndRideDurationRatio() {
    return parkAndRideDurationRatio;
  }

  public DoubleAlgorithmFunction nonTransitGeneralizedCostLimit() {
    return nonTransitGeneralizedCostLimit;
  }

  public boolean filterItinerariesWithSameFirstOrLastTrip() {
    return filterItinerariesWithSameFirstOrLastTrip;
  }

  public boolean removeItinerariesWithSameRoutesAndStops() {
    return removeItinerariesWithSameRoutesAndStops;
  }

  public boolean useAccessibilityScore() {
    return accessibilityScore;
  }

  public double minBikeParkingDistance() {
    return minBikeParkingDistance;
  }

  public static class Builder {

    private final ItineraryFilterPreferences original;
    private boolean debug;
    private double groupSimilarityKeepOne;
    private double groupSimilarityKeepThree;
    private double groupedOtherThanSameLegsMaxCostMultiplier;
    private TransitGeneralizedCostFilterParams transitGeneralizedCostLimit;
    private double bikeRentalDistanceRatio;
    private double parkAndRideDurationRatio;
    private DoubleAlgorithmFunction nonTransitGeneralizedCostLimit;
    private boolean filterItinerariesWithSameFirstOrLastTrip;
    private boolean removeItinerariesWithSameRoutesAndStops;
    private boolean accessibilityScore;
    public double minBikeParkingDistance;

    public ItineraryFilterPreferences original() {
      return original;
    }

    public Builder withDebug(boolean debug) {
      this.debug = debug;
      return this;
    }

    public Builder withGroupSimilarityKeepOne(double groupSimilarityKeepOne) {
      this.groupSimilarityKeepOne = groupSimilarityKeepOne;
      return this;
    }

    public Builder withGroupSimilarityKeepThree(double groupSimilarityKeepThree) {
      this.groupSimilarityKeepThree = groupSimilarityKeepThree;
      return this;
    }

    public Builder withGroupedOtherThanSameLegsMaxCostMultiplier(
      double groupedOtherThanSameLegsMaxCostMultiplier
    ) {
      this.groupedOtherThanSameLegsMaxCostMultiplier = groupedOtherThanSameLegsMaxCostMultiplier;
      return this;
    }

    public Builder withTransitGeneralizedCostLimit(
      TransitGeneralizedCostFilterParams transitGeneralizedCostLimit
    ) {
      this.transitGeneralizedCostLimit = transitGeneralizedCostLimit;
      return this;
    }

    public Builder withBikeRentalDistanceRatio(double bikeRentalDistanceRatio) {
      this.bikeRentalDistanceRatio = bikeRentalDistanceRatio;
      return this;
    }

    public Builder withParkAndRideDurationRatio(double parkAndRideDurationRatio) {
      this.parkAndRideDurationRatio = parkAndRideDurationRatio;
      return this;
    }

    public Builder withNonTransitGeneralizedCostLimit(
      DoubleAlgorithmFunction nonTransitGeneralizedCostLimit
    ) {
      this.nonTransitGeneralizedCostLimit = nonTransitGeneralizedCostLimit;
      return this;
    }

    public Builder withFilterItinerariesWithSameFirstOrLastTrip(
      boolean filterItinerariesWithSameFirstOrLastTrip
    ) {
      this.filterItinerariesWithSameFirstOrLastTrip = filterItinerariesWithSameFirstOrLastTrip;
      return this;
    }

    public Builder withRemoveItinerariesWithSameRoutesAndStops(
      boolean removeItinerariesWithSameRoutesAndStops
    ) {
      this.removeItinerariesWithSameRoutesAndStops = removeItinerariesWithSameRoutesAndStops;
      return this;
    }

    public Builder withAccessibilityScore(boolean accessibilityScore) {
      this.accessibilityScore = accessibilityScore;
      return this;
    }

    public Builder withMinBikeParkingDistance(double distance) {
      this.minBikeParkingDistance = distance;
      return this;
    }

    public Builder(ItineraryFilterPreferences original) {
      this.original = original;
      this.debug = original.debug;
      this.groupSimilarityKeepOne = original.groupSimilarityKeepOne;
      this.groupSimilarityKeepThree = original.groupSimilarityKeepThree;
      this.groupedOtherThanSameLegsMaxCostMultiplier =
        original.groupedOtherThanSameLegsMaxCostMultiplier;
      this.transitGeneralizedCostLimit = original.transitGeneralizedCostLimit;
      this.nonTransitGeneralizedCostLimit = original.nonTransitGeneralizedCostLimit;
      this.bikeRentalDistanceRatio = original.bikeRentalDistanceRatio;
      this.parkAndRideDurationRatio = original.parkAndRideDurationRatio;
      this.filterItinerariesWithSameFirstOrLastTrip =
        original.filterItinerariesWithSameFirstOrLastTrip;
      this.removeItinerariesWithSameRoutesAndStops =
        original.removeItinerariesWithSameRoutesAndStops;
      this.accessibilityScore = original.accessibilityScore;
      this.minBikeParkingDistance = original.minBikeParkingDistance;
    }

    public Builder apply(Consumer<Builder> body) {
      body.accept(this);
      return this;
    }

    public ItineraryFilterPreferences build() {
      var value = new ItineraryFilterPreferences(this);
      return original.equals(value) ? original : value;
    }
  }
}
