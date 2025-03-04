package org.opentripplanner.raptor.rangeraptor.transit;

import java.time.Duration;
import org.opentripplanner.raptor.api.request.DynamicSearchWindowCoefficients;
import org.opentripplanner.raptor.api.request.SearchParams;

/**
 * This is a calculator to dynamically calculate the EDT, LAT, and Raptor-search-window parameters.
 * <p>
 * NOTE ! This calculator is state-full and NOT thread-safe. Create a new one every time you need to
 * calculate theses values.
 *
 * @see DynamicSearchWindowCoefficients for the rules of operation.
 */
public class RaptorSearchWindowCalculator {

  private final int NOT_SET = -9_999_999;

  /** The coefficient to multiply with heuristicMinTransitTime */
  private final double minTransitTimeCoefficient;

  /** The coefficient to multiply with heuristicMinWaitTime */
  private final double minWaitTimeCoefficient;

  /** Min search window in seconds */
  private final Duration minSearchWindow;

  private final Duration maxSearchWindow;
  private final int stepSeconds;

  private int heuristicMinTransitTime = NOT_SET;
  private int heuristicMinWaitTime = NOT_SET;
  private int earliestDepartureTime = NOT_SET;
  private int latestArrivalTime = NOT_SET;
  private int searchWindowSeconds = NOT_SET;
  private SearchParams params;

  public RaptorSearchWindowCalculator(DynamicSearchWindowCoefficients c) {
    this.minTransitTimeCoefficient = c.minTransitTimeCoefficient();
    this.minWaitTimeCoefficient = c.minWaitTimeCoefficient();
    this.minSearchWindow = c.minWindow();
    this.maxSearchWindow = c.maxWindow();
    this.stepSeconds = c.stepMinutes() * 60;
  }

  /**
   * @return the calculated or the original value if
   */
  public int getEarliestDepartureTime() {
    return earliestDepartureTime;
  }

  public int getLatestArrivalTime() {
    return latestArrivalTime;
  }

  public int getSearchWindowSeconds() {
    return searchWindowSeconds;
  }

  public RaptorSearchWindowCalculator withHeuristics(int minTransitTime, int minWaitTime) {
    this.heuristicMinTransitTime = minTransitTime;
    this.heuristicMinWaitTime = minWaitTime;
    return this;
  }

  public RaptorSearchWindowCalculator withSearchParams(SearchParams params) {
    this.params = params;
    this.searchWindowSeconds = params.searchWindowInSeconds();
    this.earliestDepartureTime = params.earliestDepartureTime();
    this.latestArrivalTime = params.latestArrivalTime();
    return this;
  }

  public RaptorSearchWindowCalculator calculate() {
    if (heuristicMinTransitTime == NOT_SET) {
      throw new IllegalArgumentException("The minTravelTime is not set.");
    }

    if (!params.isSearchWindowSet()) {
      searchWindowSeconds = calculateSearchWindow();
    }

    // TravelWindow is the time from the earliest-departure-time to the latest-arrival-time
    int travelWindow = searchWindowSeconds + roundDownToNearestMinute(heuristicMinTransitTime);

    if (!params.isEarliestDepartureTimeSet()) {
      earliestDepartureTime = latestArrivalTime - travelWindow;
    }
    return this;
  }

  int roundDownToNearestMinute(int minTravelTimeInSeconds) {
    if (minTravelTimeInSeconds < 0) {
      throw new IllegalArgumentException(
        "This operation is not defined for negative numbers: " + minTravelTimeInSeconds
      );
    }
    // We want: 0 -> 0, 59 -> 0, 60 -> 60 ...
    return (minTravelTimeInSeconds / 60) * 60;
  }

  /**
   * Round values to closest increment of given {@code stepSeconds}. This is used to round of a time
   * or duration to the closest "step" of like 10 minutes.
   */
  int roundStep(double value) {
    return (int) Math.round(value / stepSeconds) * stepSeconds;
  }

  /**
   * Calculate travel-window using search-window and minTravelTime. The travel-window is defined by
   * the time between the EDT and LAT. The unit is seconds.
   */
  private int calculateSearchWindow() {
    // If both EDT and LAT is set the search window is the time between these, minus the
    // min-travel-time.
    if (params.isEarliestDepartureTimeSet() && params.isLatestArrivalTimeSet()) {
      int travelWindow = params.latestArrivalTime() - params.earliestDepartureTime();
      // There is no upper limit the search window when both EDT and LAT is set.
      return roundStep(travelWindow - heuristicMinTransitTime);
    } else {
      // Set the search window using the min-travel-time.
      int v = roundStep(
        minSearchWindow.toSeconds() +
        minTransitTimeCoefficient * heuristicMinTransitTime +
        minWaitTimeCoefficient * heuristicMinWaitTime
      );

      // Set an upper bound to the search window
      return (int) Math.min(maxSearchWindow.toSeconds(), v);
    }
  }
}
