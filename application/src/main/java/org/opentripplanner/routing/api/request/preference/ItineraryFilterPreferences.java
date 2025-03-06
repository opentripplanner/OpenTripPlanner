package org.opentripplanner.routing.api.request.preference;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Consumer;
import org.opentripplanner.framework.model.Units;
import org.opentripplanner.routing.algorithm.filterchain.api.TransitGeneralizedCostFilterParams;
import org.opentripplanner.routing.api.request.framework.CostLinearFunction;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Group by Similarity filter parameters. See the configuration for documentation of each field.
 *
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
 */
public final class ItineraryFilterPreferences {

  public static final ItineraryFilterPreferences DEFAULT = new ItineraryFilterPreferences();

  private final boolean accessibilityScore;
  private final double bikeRentalDistanceRatio;
  private final ItineraryFilterDebugProfile debug;
  private final boolean filterItinerariesWithSameFirstOrLastTrip;
  private final double groupedOtherThanSameLegsMaxCostMultiplier;
  private final double groupSimilarityKeepOne;
  private final double groupSimilarityKeepThree;
  private final double minBikeParkingDistance;
  private final CostLinearFunction nonTransitGeneralizedCostLimit;
  private final double parkAndRideDurationRatio;
  private final boolean removeItinerariesWithSameRoutesAndStops;
  private final TransitGeneralizedCostFilterParams transitGeneralizedCostLimit;
  private final CostLinearFunction removeTransitWithHigherCostThanBestOnStreetOnly;
  private final boolean filterDirectFlexBySearchWindow;

  private ItineraryFilterPreferences() {
    this.accessibilityScore = false;
    this.bikeRentalDistanceRatio = 0.0;
    this.debug = ItineraryFilterDebugProfile.OFF;
    this.filterItinerariesWithSameFirstOrLastTrip = false;
    this.groupedOtherThanSameLegsMaxCostMultiplier = 2.0;
    this.groupSimilarityKeepOne = 0.85;
    this.groupSimilarityKeepThree = 0.68;
    this.minBikeParkingDistance = 0;
    this.nonTransitGeneralizedCostLimit = CostLinearFunction.of(Duration.ofHours(1), 2.0);
    this.parkAndRideDurationRatio = 0.0;
    this.removeItinerariesWithSameRoutesAndStops = false;
    this.transitGeneralizedCostLimit = new TransitGeneralizedCostFilterParams(
      CostLinearFunction.of(Duration.ofMinutes(15), 1.5),
      0.4
    );
    this.removeTransitWithHigherCostThanBestOnStreetOnly = CostLinearFunction.of(
      Duration.ofMinutes(1),
      1.3
    );
    this.filterDirectFlexBySearchWindow = true;
  }

  private ItineraryFilterPreferences(Builder builder) {
    this.accessibilityScore = builder.accessibilityScore;
    this.bikeRentalDistanceRatio = Units.ratio(builder.bikeRentalDistanceRatio);
    this.debug = builder.debug;
    this.filterItinerariesWithSameFirstOrLastTrip =
      builder.filterItinerariesWithSameFirstOrLastTrip;
    this.groupedOtherThanSameLegsMaxCostMultiplier = Units.reluctance(
      builder.groupedOtherThanSameLegsMaxCostMultiplier
    );
    this.groupSimilarityKeepOne = Units.reluctance(builder.groupSimilarityKeepOne);
    this.groupSimilarityKeepThree = Units.reluctance(builder.groupSimilarityKeepThree);
    this.minBikeParkingDistance = builder.minBikeParkingDistance;
    this.nonTransitGeneralizedCostLimit = Objects.requireNonNull(
      builder.nonTransitGeneralizedCostLimit
    );
    this.parkAndRideDurationRatio = Units.ratio(builder.parkAndRideDurationRatio);
    this.removeItinerariesWithSameRoutesAndStops = builder.removeItinerariesWithSameRoutesAndStops;
    this.transitGeneralizedCostLimit = Objects.requireNonNull(builder.transitGeneralizedCostLimit);
    this.removeTransitWithHigherCostThanBestOnStreetOnly = Objects.requireNonNull(
      builder.removeTransitWithHigherCostThanBestOnStreetOnly
    );
    this.filterDirectFlexBySearchWindow = builder.filterDirectFlexBySearchWindow;
  }

  public static Builder of() {
    return DEFAULT.copyOf();
  }

  public Builder copyOf() {
    return new Builder(this);
  }

  public boolean useAccessibilityScore() {
    return accessibilityScore;
  }

  public double bikeRentalDistanceRatio() {
    return bikeRentalDistanceRatio;
  }

  public ItineraryFilterDebugProfile debug() {
    return debug;
  }

  public boolean filterItinerariesWithSameFirstOrLastTrip() {
    return filterItinerariesWithSameFirstOrLastTrip;
  }

  public double groupedOtherThanSameLegsMaxCostMultiplier() {
    return groupedOtherThanSameLegsMaxCostMultiplier;
  }

  public double groupSimilarityKeepOne() {
    return groupSimilarityKeepOne;
  }

  public double groupSimilarityKeepThree() {
    return groupSimilarityKeepThree;
  }

  public double minBikeParkingDistance() {
    return minBikeParkingDistance;
  }

  public CostLinearFunction nonTransitGeneralizedCostLimit() {
    return nonTransitGeneralizedCostLimit;
  }

  public double parkAndRideDurationRatio() {
    return parkAndRideDurationRatio;
  }

  public boolean removeItinerariesWithSameRoutesAndStops() {
    return removeItinerariesWithSameRoutesAndStops;
  }

  public TransitGeneralizedCostFilterParams transitGeneralizedCostLimit() {
    return transitGeneralizedCostLimit;
  }

  public CostLinearFunction removeTransitWithHigherCostThanBestOnStreetOnly() {
    return removeTransitWithHigherCostThanBestOnStreetOnly;
  }

  public boolean filterDirectFlexBySearchWindow() {
    return filterDirectFlexBySearchWindow;
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(ItineraryFilterPreferences.class)
      .addBoolIfTrue("accessibilityScore", accessibilityScore)
      .addNum("bikeRentalDistanceRatio", bikeRentalDistanceRatio, DEFAULT.bikeRentalDistanceRatio)
      .addEnum("debug", debug, ItineraryFilterDebugProfile.OFF)
      .addBoolIfTrue(
        "filterItinerariesWithSameFirstOrLastTrip",
        filterItinerariesWithSameFirstOrLastTrip
      )
      .addNum(
        "groupedOtherThanSameLegsMaxCostMultiplier",
        groupedOtherThanSameLegsMaxCostMultiplier,
        DEFAULT.groupedOtherThanSameLegsMaxCostMultiplier
      )
      .addNum("groupSimilarityKeepOne", groupSimilarityKeepOne, DEFAULT.groupSimilarityKeepOne)
      .addNum(
        "groupSimilarityKeepThree",
        groupSimilarityKeepThree,
        DEFAULT.groupSimilarityKeepThree
      )
      .addNum("minBikeParkingDistance", minBikeParkingDistance, DEFAULT.minBikeParkingDistance)
      .addObj(
        "nonTransitGeneralizedCostLimit",
        nonTransitGeneralizedCostLimit,
        DEFAULT.nonTransitGeneralizedCostLimit
      )
      .addNum(
        "parkAndRideDurationRatio",
        parkAndRideDurationRatio,
        DEFAULT.parkAndRideDurationRatio
      )
      .addObj(
        "transitGeneralizedCostLimit",
        transitGeneralizedCostLimit,
        DEFAULT.transitGeneralizedCostLimit
      )
      .addObj(
        "removeTransitWithHigherCostThanBestOnStreetOnly",
        removeTransitWithHigherCostThanBestOnStreetOnly,
        DEFAULT.removeTransitWithHigherCostThanBestOnStreetOnly
      )
      .addBoolIfTrue(
        "removeItinerariesWithSameRoutesAndStops",
        removeItinerariesWithSameRoutesAndStops
      )
      .addBoolIfTrue("filterDirectFlexBySearchWindow", filterDirectFlexBySearchWindow)
      .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ItineraryFilterPreferences that = (ItineraryFilterPreferences) o;
    return (
      accessibilityScore == that.accessibilityScore &&
      Double.compare(that.bikeRentalDistanceRatio, bikeRentalDistanceRatio) == 0 &&
      debug == that.debug &&
      filterItinerariesWithSameFirstOrLastTrip == that.filterItinerariesWithSameFirstOrLastTrip &&
      Double.compare(
        that.groupedOtherThanSameLegsMaxCostMultiplier,
        groupedOtherThanSameLegsMaxCostMultiplier
      ) ==
      0 &&
      Double.compare(that.groupSimilarityKeepOne, groupSimilarityKeepOne) == 0 &&
      Double.compare(that.groupSimilarityKeepThree, groupSimilarityKeepThree) == 0 &&
      Double.compare(that.minBikeParkingDistance, minBikeParkingDistance) == 0 &&
      Double.compare(that.parkAndRideDurationRatio, parkAndRideDurationRatio) == 0 &&
      removeItinerariesWithSameRoutesAndStops == that.removeItinerariesWithSameRoutesAndStops &&
      Objects.equals(nonTransitGeneralizedCostLimit, that.nonTransitGeneralizedCostLimit) &&
      Objects.equals(
        removeTransitWithHigherCostThanBestOnStreetOnly,
        that.removeTransitWithHigherCostThanBestOnStreetOnly
      ) &&
      Objects.equals(transitGeneralizedCostLimit, that.transitGeneralizedCostLimit) &&
      filterDirectFlexBySearchWindow == that.filterDirectFlexBySearchWindow
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(
      accessibilityScore,
      bikeRentalDistanceRatio,
      debug,
      filterItinerariesWithSameFirstOrLastTrip,
      groupedOtherThanSameLegsMaxCostMultiplier,
      groupSimilarityKeepOne,
      groupSimilarityKeepThree,
      minBikeParkingDistance,
      nonTransitGeneralizedCostLimit,
      parkAndRideDurationRatio,
      removeItinerariesWithSameRoutesAndStops,
      transitGeneralizedCostLimit,
      removeTransitWithHigherCostThanBestOnStreetOnly,
      filterDirectFlexBySearchWindow
    );
  }

  public static class Builder {

    private final ItineraryFilterPreferences original;
    private boolean accessibilityScore;
    private double bikeRentalDistanceRatio;
    private ItineraryFilterDebugProfile debug;
    private boolean filterItinerariesWithSameFirstOrLastTrip;
    private double groupedOtherThanSameLegsMaxCostMultiplier;
    private double groupSimilarityKeepOne;
    private double groupSimilarityKeepThree;
    public double minBikeParkingDistance;
    private CostLinearFunction nonTransitGeneralizedCostLimit;
    private double parkAndRideDurationRatio;
    private boolean removeItinerariesWithSameRoutesAndStops;
    private TransitGeneralizedCostFilterParams transitGeneralizedCostLimit;
    private CostLinearFunction removeTransitWithHigherCostThanBestOnStreetOnly;
    private boolean filterDirectFlexBySearchWindow;

    public ItineraryFilterPreferences original() {
      return original;
    }

    public Builder withAccessibilityScore(boolean accessibilityScore) {
      this.accessibilityScore = accessibilityScore;
      return this;
    }

    public Builder withBikeRentalDistanceRatio(double bikeRentalDistanceRatio) {
      this.bikeRentalDistanceRatio = bikeRentalDistanceRatio;
      return this;
    }

    public Builder withDebug(ItineraryFilterDebugProfile debug) {
      this.debug = debug;
      return this;
    }

    public Builder withFilterItinerariesWithSameFirstOrLastTrip(
      boolean filterItinerariesWithSameFirstOrLastTrip
    ) {
      this.filterItinerariesWithSameFirstOrLastTrip = filterItinerariesWithSameFirstOrLastTrip;
      return this;
    }

    public Builder withGroupedOtherThanSameLegsMaxCostMultiplier(
      double groupedOtherThanSameLegsMaxCostMultiplier
    ) {
      this.groupedOtherThanSameLegsMaxCostMultiplier = groupedOtherThanSameLegsMaxCostMultiplier;
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

    public Builder withMinBikeParkingDistance(double distance) {
      this.minBikeParkingDistance = distance;
      return this;
    }

    public Builder withNonTransitGeneralizedCostLimit(
      CostLinearFunction nonTransitGeneralizedCostLimit
    ) {
      this.nonTransitGeneralizedCostLimit = nonTransitGeneralizedCostLimit;
      return this;
    }

    public Builder withParkAndRideDurationRatio(double parkAndRideDurationRatio) {
      this.parkAndRideDurationRatio = parkAndRideDurationRatio;
      return this;
    }

    public Builder withRemoveItinerariesWithSameRoutesAndStops(
      boolean removeItinerariesWithSameRoutesAndStops
    ) {
      this.removeItinerariesWithSameRoutesAndStops = removeItinerariesWithSameRoutesAndStops;
      return this;
    }

    public Builder withTransitGeneralizedCostLimit(
      TransitGeneralizedCostFilterParams transitGeneralizedCostLimit
    ) {
      this.transitGeneralizedCostLimit = transitGeneralizedCostLimit;
      return this;
    }

    public Builder withRemoveTransitWithHigherCostThanBestOnStreetOnly(
      CostLinearFunction removeTransitWithHigherCostThanBestOnStreetOnly
    ) {
      this.removeTransitWithHigherCostThanBestOnStreetOnly =
        removeTransitWithHigherCostThanBestOnStreetOnly;
      return this;
    }

    public Builder(ItineraryFilterPreferences original) {
      this.original = original;
      this.accessibilityScore = original.accessibilityScore;
      this.bikeRentalDistanceRatio = original.bikeRentalDistanceRatio;
      this.debug = original.debug;
      this.filterItinerariesWithSameFirstOrLastTrip =
        original.filterItinerariesWithSameFirstOrLastTrip;
      this.groupedOtherThanSameLegsMaxCostMultiplier =
        original.groupedOtherThanSameLegsMaxCostMultiplier;
      this.groupSimilarityKeepOne = original.groupSimilarityKeepOne;
      this.groupSimilarityKeepThree = original.groupSimilarityKeepThree;
      this.minBikeParkingDistance = original.minBikeParkingDistance;
      this.nonTransitGeneralizedCostLimit = original.nonTransitGeneralizedCostLimit;
      this.parkAndRideDurationRatio = original.parkAndRideDurationRatio;
      this.removeItinerariesWithSameRoutesAndStops =
        original.removeItinerariesWithSameRoutesAndStops;
      this.transitGeneralizedCostLimit = original.transitGeneralizedCostLimit;
      this.removeTransitWithHigherCostThanBestOnStreetOnly =
        original.removeTransitWithHigherCostThanBestOnStreetOnly;
      this.filterDirectFlexBySearchWindow = original.filterDirectFlexBySearchWindow;
    }

    public Builder apply(Consumer<Builder> body) {
      body.accept(this);
      return this;
    }

    public ItineraryFilterPreferences build() {
      var value = new ItineraryFilterPreferences(this);
      return original.equals(value) ? original : value;
    }

    public Builder withFilterDirectFlexBySearchWindow(boolean filterDirectFlexBySearchWindow) {
      this.filterDirectFlexBySearchWindow = filterDirectFlexBySearchWindow;
      return this;
    }
  }
}
