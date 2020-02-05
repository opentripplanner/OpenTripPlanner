package org.opentripplanner.transit.raptor.rangeraptor.transit;

import org.opentripplanner.transit.raptor.api.request.DynamicSearchWindowCoefficients;
import org.opentripplanner.transit.raptor.api.request.SearchParams;


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

    /** Min search window in seconds */
    private final int c;

    /** Min trip time coefficient */
    private final float t;
    private final int stepInSeconds;

    private int minTravelTime = NOT_SET;
    private int earliestDepartureTime = NOT_SET;
    private int latestArrivalTime = NOT_SET;
    private int searchWindow = NOT_SET;
    private SearchParams params;

    public RaptorSearchWindowCalculator(DynamicSearchWindowCoefficients c) {
        this(c.c(), c.t(), c.n());
    }

    public RaptorSearchWindowCalculator(
            int minSearchWindowInMinutes, float minTripTimeCoefficient, int roundOffInMinutes
    ) {
        this.t = minTripTimeCoefficient;
        // Convert minutes to seconds
        this.c = minSearchWindowInMinutes * 60;
        this.stepInSeconds = roundOffInMinutes * 60;
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

    public int getSearchWindowInSeconds() {
        return searchWindow;
    }

    public RaptorSearchWindowCalculator withMinTripTime(int minTravelTime) {
        this.minTravelTime = minTravelTime;
        return this;
    }

    public RaptorSearchWindowCalculator withSearchParams(SearchParams params) {
        this.params = params;
        this.searchWindow = params.searchWindowInSeconds();
        this.earliestDepartureTime = params.earliestDepartureTime();
        this.latestArrivalTime = params.latestArrivalTime();
        return this;
    }

    public RaptorSearchWindowCalculator calculate() {
        if(minTravelTime == NOT_SET) {
            throw new IllegalArgumentException("The minTravelTime is not set.");
        }

        if (!params.isSearchWindowSet()) {
            searchWindow = calculateSearchWindow();
        }

        if (!params.isLatestArrivalTimeSet()) {
            latestArrivalTime = earliestDepartureTime + (
                    searchWindow + roundUpToNearestMinute(minTravelTime)
            );
        }
        if (!params.isEarliestDepartureTimeSet()) {
            earliestDepartureTime = latestArrivalTime - (
                    searchWindow + roundUpToNearestMinute(minTravelTime)
            );
        }
        return this;
    }

    int roundUpToNearestMinute(int minTravelTimeInSeconds) {
        if(minTravelTimeInSeconds < 0) {
            throw new IllegalArgumentException(
                    "This operation is not defined for negative numbers."
            );
        }
        // See the UnitTest for verification of this:
        // We want: 0 -> 0, 1 -> 60, 59 -> 60 ...
        return ((minTravelTimeInSeconds + 59) / 60) * 60;
    }

    /**
     * Calculate travel-window using search-window and minTravelTime. The travel-window is defined
     * by the time between the EDT and LAT. The unit is seconds.
     */
    private int calculateSearchWindow() {
        return roundStep(c + t * minTravelTime);
    }

    /**
     * Round values to closest increment of given {@code stepInSeconds}. This is used to round of a
     * time or duration to the closest "step" of like 10 minutes.
     */
    int roundStep(float value) {
        return Math.round(value / stepInSeconds) * stepInSeconds;
    }
}
