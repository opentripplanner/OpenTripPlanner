package org.opentripplanner.routing.api.request.preference;

import java.util.Objects;
import java.util.function.Consumer;
import org.opentripplanner.ext.accessibilityscore.AccessibilityScoreFilter;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilterChainBuilder;
import org.opentripplanner.routing.algorithm.filterchain.api.TransitGeneralizedCostFilterParams;
import org.opentripplanner.routing.api.request.framework.DoubleAlgorithmFunction;
import org.opentripplanner.routing.api.request.framework.RequestFunctions;
import org.opentripplanner.routing.api.request.framework.Units;

/**
 * Group by Similarity filter parameters
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
  }

  public static Builder of() {
    return DEFAULT.copyOf();
  }

  public Builder copyOf() {
    return new Builder(this);
  }

  /**
   * Switch on to return all itineraries and mark filtered itineraries as deleted.
   */
  public boolean debug() {
    return debug;
  }

  /**
   * Keep ONE itinerary for each group with at least this part of the legs in common. Default value
   * is 0.85 (85%), use a value less than 0.50 to turn off.
   *
   * @see ItineraryListFilterChainBuilder#addGroupBySimilarity(org.opentripplanner.routing.algorithm.filterchain.GroupBySimilarity)
   */
  public double groupSimilarityKeepOne() {
    return groupSimilarityKeepOne;
  }

  /**
   * Keep maximum THREE itineraries for each group with at least this part of the legs in common.
   * Default value is 0.68 (68%), use a value less than 0.50 to turn off.
   */
  public double groupSimilarityKeepThree() {
    return groupSimilarityKeepThree;
  }

  /**
   * Of the itineraries grouped to maximum of three itineraries, how much worse can the non-grouped
   * legs be compared to the lowest cost. 2.0 means that they can be double the cost, and any
   * itineraries having a higher cost will be filtered. Default value is 2.0, use a value lower than
   * 1.0 to turn off
   */
  public double groupedOtherThanSameLegsMaxCostMultiplier() {
    return groupedOtherThanSameLegsMaxCostMultiplier;
  }

  /**
   * A relative maximum limit for the generalized cost for transit itineraries.
   */
  public TransitGeneralizedCostFilterParams transitGeneralizedCostLimit() {
    return transitGeneralizedCostLimit;
  }

  /**
   * This is used to filter out bike rental itineraries that contain mostly walking. The value
   * describes the ratio of the total itinerary that has to consist of bike rental to allow the
   * itinerary.
   * <p>
   * Default value is off (0). If you want a minimum of 30% cycling, use a value of 0.3.
   */
  public double bikeRentalDistanceRatio() {
    return bikeRentalDistanceRatio;
  }

  /**
   * This is used to filter out park and ride itineraries that contain only driving plus a very long
   * walk. The value describes the ratio of the total itinerary duration that has to consist of
   * driving to allow the itinerary.
   * <p>
   * Default value is 0.3 (30%), use a value of 0 to turn off.
   */
  public double parkAndRideDurationRatio() {
    return parkAndRideDurationRatio;
  }

  /**
   * This is a a bit similar to {@link #transitGeneralizedCostLimit}, with a few important
   * differences.
   * <p>
   * This function is used to compute a max-limit for generalized-cost. The limit is applied to
   * itineraries with no transit legs, however ALL itineraries (including those with transit legs)
   * are considered when calculating the minimum cost.
   * <p>
   * The smallest generalized-cost value is used as input to the function. For example if the
   * function is {@code f(x) = 1800 + 2.0 x} and the smallest cost is {@code 5000}, then all
   * non-transit itineraries with a cost larger than {@code 1800 + 2 * 5000 = 11 800} is dropped.
   * <p>
   * The default is {@code 3600 + 2x} - 1 hours plus 2 times the lowest cost.
   */
  public DoubleAlgorithmFunction nonTransitGeneralizedCostLimit() {
    return nonTransitGeneralizedCostLimit;
  }

  /**
   * This is used to filter out journeys that have either same first or last trip. If two journeys
   * starts or ends with exactly same transit leg (same trip id and same service day), one of them
   * will be filtered out.
   */
  public boolean filterItinerariesWithSameFirstOrLastTrip() {
    return filterItinerariesWithSameFirstOrLastTrip;
  }

  /**
   * Whether to compute the sandbox accessibility score currently being tested at IBI.
   * <p>
   * {@link AccessibilityScoreFilter}
   */
  public boolean useAccessibilityScore() {
    return accessibilityScore;
  }

  /**
   * Whether to remove timeshifted "duplicate" itineraries from the search results so that you get a
   * greater variety of results rather than the same ones at different times.
   */
  public boolean removeItinerariesWithSameRoutesAndStops() {
    return removeItinerariesWithSameRoutesAndStops;
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
    private boolean accessibilityScore;
    private boolean removeItinerariesWithSameRoutesAndStops;

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

    public Builder withAccessibilityScore(boolean accessibilityScore) {
      this.accessibilityScore = accessibilityScore;
      return this;
    }

    public Builder withRemoveItinerariesWithSameRoutesAndStops(
      boolean removeItinerariesWithSameRoutesAndStops
    ) {
      this.removeItinerariesWithSameRoutesAndStops = removeItinerariesWithSameRoutesAndStops;
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
      this.accessibilityScore = original.accessibilityScore;
      this.removeItinerariesWithSameRoutesAndStops =
        original.removeItinerariesWithSameRoutesAndStops;
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
