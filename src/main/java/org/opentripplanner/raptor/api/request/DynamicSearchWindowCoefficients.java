package org.opentripplanner.raptor.api.request;

import java.time.Duration;

/**
 * The dynamic search window coefficients is used to calculate EDT(earliest-departure-time),
 * LAT(latest-arrival-time) and SW(raptor-search-window) request parameters using heuristics. The
 * heuristics perform a Raptor search (one-iteration) to find a trip which we use to find a lower
 * bound for the travel duration time - the "minTransitTime". The heuristic search is used for other
 * purposes too, and is very fast.
 * <p>
 * At least the EDT or the LAT must be passed into Raptor to perform a Range Raptor search. If
 * unknown/missing the parameters(EDT, LAT, DW) is dynamically calculated. The dynamic coefficients
 * affect the performance and should be tuned to match the deployment.
 * <p>
 * The request parameters are calculated like this:
 * <pre>
 *     DW  = round_N(C + T * minTransitTime + W * minWaitTime)
 *     LAT = EDT + DW + minTransitTime
 *     EDT = LAT - (DW + minTransitTime)
 * </pre>
 * The {@code round_N(...)} method is will round the input to the closest multiplication of N.
 * <p>
 * The 3 coefficients above are:
 * <ol>
 *     <li>{@code C} - {@link #minWindow()}</li>
 *     <li>{@code T} - {@link #minTransitTimeCoefficient()}</li>
 *     <li>{@code W} - {@link #minWaitTimeCoefficient()}</li>
 *     <li>{@code N} - {@link #stepMinutes()}</li>
 * </ol>
 * In addition the this an upper bound on the calculation of the search window:
 * {@link #maxWindow()}.
 */
public interface DynamicSearchWindowCoefficients {
  /**
   * {@code T} - The coefficient to multiply with {@code minTransitTime}. Use a value between {@code
   * 0.0} to {@code 3.0}. Using {@code 0.0} will eliminate the {@code minTransitTime} from the
   * dynamic raptor-search-window calculation.
   */
  default double minTransitTimeCoefficient() {
    return 0.5f;
  }

  /**
   * {@code T} - The coefficient to multiply with {@code minWaitTime}. Use a value between {@code
   * 0.0} to {@code 1.0}. Using {@code 0.0} will eliminate the {@code minWaitTime} from the dynamic
   * raptor-search-window calculation.
   */
  default double minWaitTimeCoefficient() {
    return 0.5f;
  }

  /**
   * {@code C} - The constant minimum number of minutes for a raptor search window. Use a value
   * between 20-180 minutes in a normal deployment.
   */
  default Duration minWindow() {
    return Duration.ofMinutes(40);
  }

  /**
   * Set an upper limit to the calculation of the dynamic search window to prevent exceptionable
   * cases to cause very long search windows. Long search windows consumes a lot of resources and
   * may take a long time. Use this parameter to tune the desired maximum search time.
   * <p>
   * This is the parameter that affect the response time most, the downside is that a search is only
   * guaranteed to be pareto-optimal within a search-window.
   * <p>
   * The default is 3 hours. The unit is minutes.
   */
  default Duration maxWindow() {
    return Duration.ofHours(3);
  }

  /**
   * {@code N} - The search window is rounded of to the closest multiplication of N minutes. If N=10
   * minutes, the search-window can be 10, 20, 30 ... minutes. It the computed search-window is 5
   * minutes and 17 seconds it will be rounded up to 10 minutes.
   * <p/>
   * Use a value between {@code 1 and 60}. This should be less than the {@code C}
   * (min-raptor-search-window) coefficient.
   */
  default int stepMinutes() {
    return 10;
  }
}
