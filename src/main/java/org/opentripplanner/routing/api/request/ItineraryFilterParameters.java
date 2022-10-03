package org.opentripplanner.routing.api.request;

import java.util.function.Consumer;
import org.opentripplanner.ext.accessibilityscore.AccessibilityScoreFilter;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilterChainBuilder;
import org.opentripplanner.routing.algorithm.filterchain.api.TransitGeneralizedCostFilterParams;
import org.opentripplanner.routing.api.request.framework.DoubleAlgorithmFunction;
import org.opentripplanner.routing.api.request.framework.RequestFunctions;

/**
 * Group by Similarity filter parameters
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
 */
public final class ItineraryFilterParameters {

  public static final ItineraryFilterParameters DEFAULT = new ItineraryFilterParameters();

  /**
   * Switch on to return all itineraries and mark filtered itineraries as deleted.
   */
  public final boolean debug;

  /**
   * Keep ONE itinerary for each group with at least this part of the legs in common. Default value
   * is 0.85 (85%), use a value less than 0.50 to turn off.
   *
   * @see ItineraryListFilterChainBuilder#addGroupBySimilarity(org.opentripplanner.routing.algorithm.filterchain.GroupBySimilarity)
   */
  public final double groupSimilarityKeepOne;

  /**
   * Keep maximum THREE itineraries for each group with at least this part of the legs in common.
   * Default value is 0.68 (68%), use a value less than 0.50 to turn off.
   */
  public final double groupSimilarityKeepThree;

  /**
   * Of the itineraries grouped to maximum of three itineraries, how much worse can the non-grouped
   * legs be compared to the lowest cost. 2.0 means that they can be double the cost, and any
   * itineraries having a higher cost will be filtered. Default value is 2.0, use a value lower than
   * 1.0 to turn off
   */
  public final double groupedOtherThanSameLegsMaxCostMultiplier;

  /**
   * A relative maximum limit for the generalized cost for transit itineraries.
   */
  public final TransitGeneralizedCostFilterParams transitGeneralizedCostLimit;

  /**
   * This is used to filter out bike rental itineraries that contain mostly walking. The value
   * describes the ratio of the total itinerary that has to consist of bike rental to allow the
   * itinerary.
   * <p>
   * Default value is off (0). If you want a minimum of 30% cycling, use a value of 0.3.
   */
  public final double bikeRentalDistanceRatio;

  /**
   * This is used to filter out park and ride itineraries that contain only driving plus a very long
   * walk. The value describes the ratio of the total itinerary duration that has to consist of
   * driving to allow the itinerary.
   * <p>
   * Default value is 0.3 (30%), use a value of 0 to turn off.
   */
  public final double parkAndRideDurationRatio;

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
  public final DoubleAlgorithmFunction nonTransitGeneralizedCostLimit;

  /**
   * This is used to filter out journeys that have either same first or last trip. If two journeys
   * starts or ends with exactly same transit leg (same trip id and same service day), one of them
   * will be filtered out.
   */
  public final boolean filterItinerariesWithSameFirstOrLastTrip;

  /**
   * Whether to compute the sandbox accessibility score currently being tested at IBI.
   * <p>
   * {@link AccessibilityScoreFilter}
   */
  public final boolean accessibilityScore;

  /**
   * Whether to remove timeshifted "duplicate" itineraries from the search results so that you get a
   * greater variety of results rather than the same ones at different times.
   */
  public final boolean removeItinerariesWithSameRoutesAndStops;

  private ItineraryFilterParameters() {
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

  private ItineraryFilterParameters(Builder builder) {
    this.debug = builder.debug;
    this.groupSimilarityKeepOne = builder.groupSimilarityKeepOne;
    this.groupSimilarityKeepThree = builder.groupSimilarityKeepThree;
    this.groupedOtherThanSameLegsMaxCostMultiplier =
      builder.groupedOtherThanSameLegsMaxCostMultiplier;
    this.transitGeneralizedCostLimit = builder.transitGeneralizedCostLimit;
    this.nonTransitGeneralizedCostLimit = builder.nonTransitGeneralizedCostLimit;
    this.bikeRentalDistanceRatio = builder.bikeRentalDistanceRatio;
    this.parkAndRideDurationRatio = builder.parkAndRideDurationRatio;
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

  public static ItineraryFilterParameters createDefault() {
    return new ItineraryFilterParameters();
  }

  public static class Builder {

    private final ItineraryFilterParameters original;
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

    public ItineraryFilterParameters original() {
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

    public Builder(ItineraryFilterParameters original) {
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

    public ItineraryFilterParameters build() {
      var value = new ItineraryFilterParameters(this);
      return original.equals(value) ? original : value;
    }
  }
}
