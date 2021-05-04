package org.opentripplanner.routing.algorithm.transferoptimization.services;

import java.util.Collection;
import java.util.function.ToIntFunction;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.path.PathLeg;
import org.opentripplanner.transit.raptor.api.transit.RaptorSlackProvider;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.util.time.DurationUtils;

/**
 * This is a calculator to calculate a min-safe-transfer-time. The
 * min-safe-transfer-time is used to apply extra cost to tight transfers.
 * <p>
 * The min-safe-transfer-time is calculated using the <em>minimum transit time</em>
 * across the list of journeys passed in. The transit time is used because we do not
 * want to include waiting time, witch variate more from search to search.
 * <p>
 * Example:
 * <pre>
 *   upper limit: 40 minutes
 *   minSafeTransferTimeFactor: f = 6.67
 *
 *   Itineraries returned from search:
 *     - I1: Train R1 10:00 16:24
 *     - I2: Train R1 10:00 15:00 ~ A ~ Bus L1 15:05 16:05
 *     - I3: Train R1 10:00 15:05 ~ B ~ Bus L2 15:15 16:12
 *
 *   Calculate the  min-safe-transfer-time relative to transit-time:
 *
 *      min transit-time across all itineraries:  min(6h30m, 6h, 6h) = 6h
 *      min-safe-transfer-time:  T = 6h * 6.67% = 24m
 *
 *   This can then be used to give all transfers less than 24 minutes an extra cost.
 * </pre>
 *
 * The table below show the min-safe-transfer-time for some example min-total-transit-times:
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
public class MinSafeTransferTimeCalculator<T extends RaptorTripSchedule> {

  private final RaptorSlackProvider slackProvider;

  /**
   * Min-safe-transfer-time is defined as P=6.67% of total-transit-time, maximum 40 minutes.
   * There is no need to put a lower bound on this.
   */
  private static final double P = 100.0 / 15.0;

  /**
   * This is an upper bound for min-safe-transfer-time. Journeys that
   * last for more than 10 hours will use 40 minutes as a {@code minSafeTransferTime}.
   */
  private static final int MIN_SAFE_TRANSFER_TIME_LIMIT_UPPER_BOUND = DurationUtils.duration("40m");


  public MinSafeTransferTimeCalculator(RaptorSlackProvider slackProvider) {
    this.slackProvider = slackProvider;
  }

  public int minSafeTransferTime(Collection<Path<T>> paths) {
    ToIntFunction<Path<T>> totalTransitTimeOp = p -> p
        .legStream()
        .filter(PathLeg::isTransitLeg)
        .mapToInt(this::durationIncludingSlack)
        .sum();

    return minSafeTransferTimeOp(paths, totalTransitTimeOp);
  }

  int durationIncludingSlack(PathLeg<T>  leg)  {
    var p = leg.asTransitLeg().trip().pattern();
    return leg.duration() + slackProvider.transitSlack(p);
  }

  /**
   * Calculate the min-safe-transfer-time for the given path or itinerary.
   *
   * @param <T> The path or itinerary type for the list of journeys passed in.
   */
  public static <T> int minSafeTransferTimeOp(Collection<T> list, ToIntFunction<T> transitTimeSeconds) {
    if(list.isEmpty()) { return MIN_SAFE_TRANSFER_TIME_LIMIT_UPPER_BOUND; }
    int minTransitTime = list.stream().mapToInt(transitTimeSeconds).min().getAsInt();
    int minSafeTransitTime = (int) Math.round(minTransitTime * P / 100.0);
    return Math.min(MIN_SAFE_TRANSFER_TIME_LIMIT_UPPER_BOUND, minSafeTransitTime);
  }
}
