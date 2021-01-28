package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryFilter;

import java.util.Calendar;
import java.util.Collection;
import java.util.List;


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
 *   minSafeTransferTimeFactor: f = 2.0
 *
 *   Itineraries returned from search:
 *     - I1: Train R1 10:00 16:24 (Cost: 23040)
 *     - I2: Train R1 10:00 15:00 ~ A ~ Bus L1 15:05 16:05 (Cost: 22200)
 *     - I3: Train R1 10:00 15:05 ~ B ~ Bus L2 15:15 16:12 (Cost: 22120)
 *
 *   Calculate the  min-safe-transfer-time relative to travel-time:
 *
 *      min travel-time across all itineraries:  min(6h30m, 6h, 6h) = 6h
 *      min-safe-transfer-time:  T = 6h * 6.67% = 24m
 *
 *   All transfers less than 24 minutes will be given an extra cost.
 *
 *    Calculate adjusted cost, where w' is transfer-time for each transfer:
 *
 *      adjusted-cost = generalized-cost + f * ∑((T - w'))
 *
 *     - I1 do not have any transfers so cost is unchanged: 23 040
 *     - I2: 22 200 + 2.0 * (24m - 5m) = 22 200 + 2 * (17*60) = 24 240
 *     - I3: 22 000 + 2.0 * (24m - 10m) = 22 000 + 2 * (14*60) = 23 680
 * </pre>
 *
 * The table below show the min-safe-transfer-time for some example total travel times:
 * <pre>
 *  |   total  |    min safe   |
 *  | tr. time | transfer time |
 *  |   30m    |      2m       |
 *  |    1h    |      4m       |
 *  |    5h    |     20m       |
 *  | > 10h    |     40m       |
 * </pre>
 * Note! Normally the board-/alight-/transfer-slack serve as a lower bound for the
 *       transfer time; Hence also for the min-safe-transfer-time for short journeys.
 */
public class AddMinSafeTransferCostFilter implements ItineraryFilter {

  /**
   * Min-safe-transfer-time is defined as P=6.67% of total-travel-time, maximum 40 minutes.
   * There is no need to put a lower bound on this.
   */
  private static final double P = 20.0/3;

  /**
   * This is an upper bound for adding a penalty to short transfer times.
   * Journeys that last for more than 10 hours will ues 30 minutes as a
   * {@code minSafeTransferTime}.
   */
  public final int MIN_SAFE_TRANSFER_TIME_LIMIT_UPPER_BOUND = 40 * 60;

  /**
   * If the transfer-time for an itinerary is less than the min-safe-transfer-time-limit, then
   * the difference is multiplied with this factor and added to the adjusted-cost.
   */
  public final double minSafeTransferTimeFactor;


  public AddMinSafeTransferCostFilter(double minSafeTransferTimeFactor) {
    this.minSafeTransferTimeFactor = minSafeTransferTimeFactor;
  }

  @Override
  public String name() {
    return "add-min-safe-transfer-cost-filter";
  }

  @Override
  public List<Itinerary> filter(List<Itinerary> itineraries) {
    int minSafeTransferTime =  minSafeTransferTime(itineraries);

    for (Itinerary it : itineraries) {
      it.generalizedCost += calculateAdditionalCost(minSafeTransferTime, it);
    }
    return itineraries;
  }

  @Override
  public boolean removeItineraries() {
    return false;
  }

  @Override
  public String toString() {
    return name() + "{minSafeTransferTimeFactor: " + minSafeTransferTimeFactor + "}";
  }

  /**
   * Calculate the ideal-cost based on the given {@code minSafeTransferTime} and {@code itinerary}.
   */
  int calculateAdditionalCost(int minSafeTransferTime, Itinerary i) {
    int cost = 0;
    Leg prev = null;
    boolean firstTransit = true;

    for (Leg leg : i.legs) {
      if(leg.isScheduled()) {
        // Skip the first transit leg, we only want to add "unsafe" cost to transfers between
        // 2 scheduled legs(it might be walking in between). This is because we assume none
        // scheduled legs can be time-shifted, hence not unsafe.
        if(firstTransit) {
          firstTransit = false;
        }
        else {
          int waitTime = durationInSeconds(prev.endTime, leg.startTime);

          if (waitTime < minSafeTransferTime) {
            cost += (int) ((minSafeTransferTime - waitTime) * minSafeTransferTimeFactor);
          }
        }
      }
      prev = leg;
    }
    return cost;
  }

  /**
   * Calculate the ideal-cost based on the given {@code idealTransferTime} and {@code itinerary}.
   */
  int minSafeTransferTime(Collection<Itinerary> list) {
    int minTransitTime = list.stream()
        .mapToInt(it -> it.transitTimeSeconds)
        .min()
        .orElse(0);
    int minSafeTravelTime = (int)Math.round(minTransitTime * P / 100.0);
    return Math.min(MIN_SAFE_TRANSFER_TIME_LIMIT_UPPER_BOUND, minSafeTravelTime);
  }

  private int durationInSeconds(Calendar from, Calendar to) {
    return (int) ((to.getTimeInMillis() - from.getTimeInMillis()) / 1000);
  }
}
