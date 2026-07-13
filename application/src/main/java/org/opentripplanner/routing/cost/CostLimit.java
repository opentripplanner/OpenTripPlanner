package org.opentripplanner.routing.cost;

import org.opentripplanner.raptor.spi.RaptorConstants;
import org.opentripplanner.raptor.spi.RaptorCostConverter;
import org.opentripplanner.utils.logging.Throttle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts a street-search-derived generalized cost — the cost of an access, egress or transfer
 * leg — into a Raptor internal cost, clamping it to a sane maximum first.
 * <p>
 * These costs are produced by an A* search (or, for an edge-less transfer, a straight-line distance
 * estimate) and are not bounded by Raptor's own pruning. A leg over, for example, a steep,
 * wheelchair-inaccessible stairway can accumulate an extremely high generalized cost. Casting such a
 * cost straight to Raptor's 32-bit {@code int} representation overflows and wraps to a negative
 * value, which then makes the whole request fail (a negative cost is rejected when the path is
 * mapped back to an itinerary) and can corrupt the pareto search by making a path look artificially
 * cheap.
 * <p>
 * The limit is high enough — several days of transit-equivalent cost — that no legitimate journey
 * is affected, but far enough below {@link RaptorConstants#UNREACHED_HIGH} that summing a handful
 * of capped legs still cannot overflow. A capped leg ends up with a large-but-finite cost and is
 * dropped by the itinerary filters, so valid alternatives are returned instead of an error.
 * <p>
 * Transfers were the first place this overflow was observed and fixed
 * (<a href="https://github.com/opentripplanner/OpenTripPlanner/issues/5509">#5509</a>); access and
 * egress legs are the unguarded sibling
 * (<a href="https://github.com/opentripplanner/OpenTripPlanner/issues/7679">#7679</a>). This helper
 * holds the single shared definition used by both.
 *
 * @see RaptorCostConverter
 */
public final class CostLimit {

  private static final Logger LOG = LoggerFactory.getLogger(CostLimit.class);
  private static final Throttle THROTTLE_COST_EXCEEDED = Throttle.ofOneSecond();

  /**
   * The maximum street generalized cost, in transit-seconds, that is converted to Raptor. This is
   * roughly 23 days and converts to a Raptor cost of 2&times;10<sup>8</sup>, an order of magnitude
   * below {@link RaptorConstants#UNREACHED_HIGH}.
   */
  public static final int MAX_COST = 2_000_000;

  private CostLimit() {}

  /**
   * Convert a street generalized cost (transit-seconds) to a Raptor cost, capping it at
   * {@link #MAX_COST} so the conversion cannot overflow Raptor's {@code int} cost. A negative input
   * (which only happens if the cost has already overflowed upstream) is treated as the worst case
   * and capped as well, never as a "free" leg.
   */
  public static int toRaptorCost(double generalizedCost) {
    return RaptorCostConverter.toRaptorCost(sanityLimit(generalizedCost));
  }

  /**
   * Like {@link #toRaptorCost(double)} but truncates the cost to whole transit-seconds first. This
   * preserves the historical resolution of transfer costs — a side effect of the original integer
   * sanity-check in #5509 — so routing results are unchanged. New callers should normally use
   * {@link #toRaptorCost(double)}, which keeps centi-second precision.
   */
  public static int toRaptorCostWholeSeconds(double generalizedCost) {
    return RaptorCostConverter.toRaptorCost((int) sanityLimit(generalizedCost));
  }

  private static double sanityLimit(double cost) {
    if (cost >= 0 && cost <= MAX_COST) {
      return cost;
    }
    THROTTLE_COST_EXCEEDED.throttle(() ->
      LOG.warn(
        "Generalized cost {} for a street access, egress or transfer leg exceeded the maximum of {} and was capped. Please consider changing the cost calculation. More information: https://github.com/opentripplanner/OpenTripPlanner/pull/5516#issuecomment-1819138078",
        cost,
        MAX_COST
      )
    );
    return MAX_COST;
  }
}
