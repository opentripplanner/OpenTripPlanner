package org.opentripplanner.routing.algorithm.filterchain;

import org.opentripplanner.model.base.ToStringBuilder;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;

import javax.annotation.Nullable;
import java.util.Calendar;
import java.util.Collection;


/**
 * The adjusted cost is used to adjust the itinerary generalized-cost to better reflect
 * "human-cost". It is used to compare itineraries and choose the one to keep. It is not used
 * while routing, and it is only used to compare itineraries that is already similar.
 * <p>
 * The adjusted-cost may use parameters in the calculation that is derived from the set of
 * itineraries it is comparing; This information is not available during routing.
 * <p>
 * This version add extra cost for short transfer times on long journeys.
 *
 * <b>Extra cost for short transfer times on long journeys</b>
 * <p>
 * The adjusted-cost is calculated using the generalized-cost plus a cost for short transfer times.
 * <p>
 * Example:
 * <pre>
 *   preferredTransferTimeLongJourneys: 30 minutes
 *   lessThanIdealTransferTimeFactor: f = 2.0
 *
 *   Itineraries returned from search:
 *     - I1: Train R1 10:00 16:24 (Cost: 23040)
 *     - I2: Train R1 10:00 15:00 ~ A ~ Bus L1 15:05 16:05 (Cost: 22200)
 *     - I3: Train R1 10:00 15:05 ~ B ~ Bus L2 15:15 16:12 (Cost: 22120)
 *
 *   Calculate the "ideal" transfer time relative to travel-time:
 *
 *      travel-time:             min(6h30m, 6h, 6h) = 6h
 *      ideal-transfer-time(T):  6h * 5% = 18m
 *
 *   All transfers less than 18 minutes will be given an extra cost.
 *
 *    Calculate adjusted cost, where w' is transfer-time for each transfer:
 *
 *      adjusted-cost = generalized-cost + f * ∑((T - w'))
 *
 *     - I1 do not have any transfers so cost is unchanged: 23 040
 *     - I2: 22 200 + 2.0 * (18m - 5m) = 22 200 + 2 * 780 = 23 760
 *     - I3: 22 000 + 2.0 * (18m - 10m) = 22 000 + 2 * 480 = 23 080
 * </pre>
 *
 * The table below show the ideal-transfer-time for some example total travel times:
 * <pre>
 *  | tot-tr-time | ideal-transfer-time |
 *  |   < 40m     |      2m             |
 *  |     1h      |      3m             |
 *  |     5h      |     15m             |
 *  |   > 10h     |     30m             |
 * </pre>
 */
public class AdjustedCost {

  /**
   * Ideal-transfer-time is defined as P=5% of total-travel-time, at maximum 30 minutes.
   */
  private static final int P = 5;

  /**
   * This is used as an upper limit for adding a penalty to short transfer times.
   * <p>
   * The ideal-transfer-time-limit used is calculated by taking the:
   * <pre>
   *   ideal-transfer-time-limit = min(5% of total-travel-time, preferredTransferTimeLongJourneys)
   * </pre>
   * The calculated limit is then used to add a penalty to all transfers with a time
   * less than the limit.
   */
  public final int PREFERRED_TRANSFER_TIME_LONG_JOURNEYS = 30 * 60;

  /**
   * If the transfer-time for an itinerary is less than the ideal-transfer-time-limit, then
   * the delta is multiplied with this factor and added to the adjusted-cost.
   */
  public final float lessThanIdealTransferTimeFactor;


  private AdjustedCost (float lessThanIdealTransferTimeFactor) {
    this.lessThanIdealTransferTimeFactor = lessThanIdealTransferTimeFactor;
  }

  @Nullable
  public static AdjustedCost create(double idealTransferTimeFactor) {
    return idealTransferTimeFactor <= 0.0
        ?  null
        : new AdjustedCost((float) idealTransferTimeFactor);
  }


  /**
   * Calculate the ideal-cost based on the given {@code idealTransferTime} and {@code itinerary}.
   */
  public int calculate(int idealTransferTime, Itinerary i) {
    int cost = i.generalizedCost;
    Leg prev = null;

    for (Leg leg : i.legs) {
      if(prev != null && leg.isScheduled()) {
        int waitTime = durationInSeconds(prev.endTime, leg.startTime);

        if(waitTime < idealTransferTime) {
          cost += (int)((idealTransferTime - waitTime) * lessThanIdealTransferTimeFactor);
        }
      }
      prev = leg;
    }
    return cost;
  }

  /**
   * Calculate the ideal-cost based on the given {@code idealTransferTime} and {@code itinerary}.
   */
  public int idealTransferTime(Collection<Itinerary> list) {
    int minTransitTime = list.stream()
        .mapToInt(it -> it.transitTimeSeconds)
        .min()
        .orElse(0);
    return Math.max(PREFERRED_TRANSFER_TIME_LONG_JOURNEYS, minTransitTime * P / 100);
  }


    private int durationInSeconds(Calendar from, Calendar to) {
    return (int) ((to.getTimeInMillis() - from.getTimeInMillis()) / 1000);
  }


  @Override
  public String toString() {
    return ToStringBuilder
        .of(AdjustedCost.class)
        .addNum("lessThanIdealTransferTimeFactor", lessThanIdealTransferTimeFactor)
        .toString();
  }
}
