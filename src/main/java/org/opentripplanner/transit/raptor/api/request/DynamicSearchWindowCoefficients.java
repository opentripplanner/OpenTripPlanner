package org.opentripplanner.transit.raptor.api.request;


/**
 * The dynamic search window coefficients is used to calculate EDT(earliest-departure-time),
 * LAT(latest-arrival-time) and DW(raptor-search-window) request parameters using heuristics.
 * The heuristics perform a Raptor search (one-iteration) to find a trip witch we use to find a
 * lower bound for the travel duration time - "minTripTime". The heuristic search is used for
 * other purposes too, and is very fast.
 * <p>
 * At least EDT or LAT must be passed into Raptor to perform a Range Raptor search. If
 * unknown/missing the parameters(EDT, LAT, DW) is dynamically calculated. The dynamic
 * coefficients affect the performance and should be tuned to match the deployment.
 * <p>
 * The request parameters are calculated like this:
 * <pre>
 *     DW  = round_N(C + T * minTripTime)
 *     LAT = EDT + DW + minTripTime
 *     EDT = LAT - (DW + minTripTime)
 * </pre>
 * The {@code round_N(...)} method is will round the input to the closest multiplication of N.
 * <p>
 * There are 3 coefficients: {@link #c()}, {@link #t()} and {@link #n()}.
 */
public interface DynamicSearchWindowCoefficients {

    /**
     * {@code T} - The coefficient to multiply with {@code minTripTime}. Use a value between
     * {@code 0.0} to {@code 3.0}. Using {@code 0.0} will give you a raptor-search-window ≈
     * {@code C}.
     */
    default float t() { return 0.75f; }

    /**
     * {@code C} - The constant minimum number of minutes for a raptor search window. Use a value
     * between 30-180 minutes in a normal deployment.
     */
    default int c() { return 60; }

    /**
     * {@code N} - The search window is rounded of to the closest multiplication of N minutes.
     * If N=10 minutes, the search-window can be 10, 20, 30 ... minutes. It the computed
     * search-window is 5 minutes and 17 seconds it will be rounded up to 10 minutes.
     * <p/>
     * Use a value between {@code 1 and 60}. This should be less than the {@code C}
     * (min-raptor-search-window) coefficient.
     */
    default int n() { return 10; }
}
