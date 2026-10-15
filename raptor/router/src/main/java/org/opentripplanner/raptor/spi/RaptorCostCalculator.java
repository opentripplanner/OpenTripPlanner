package org.opentripplanner.raptor.spi;

///
/// The responsibility is to calculate multi-criteria value (like the generalized cost).
///
/// The implementation should be immutable and thread safe.
///
/// See {@link RaptorCostConverter} for resolution and units for time/duration and
/// c1(generalized-cost).
///
public interface RaptorCostCalculator<T extends RaptorTripSchedule> {
  /**
   * The cost is zero (0) if it's not calculated or if the cost "element" have no cost associated.
   * with it.
   */
  int ZERO_COST = 0;

  /**
   * Calculate cost when on-board of a trip. The cost is only used to compare to paths on the same
   * trip - so any cost that is constant for a given trip can be dropped, but it will make debugging
   * easier if the cost can be compared with the "stop-arrival-cost". The cost must incorporate the
   * fact that 2 boarding may happen at 2 different stops.
   */
  int boardingCost(
    boolean firstBoarding,
    int prevArrivalTime,
    int boardStopIndex,
    int boardTime,
    T trip,
    RaptorTransferConstraint transferConstraints
  );

  /**
   * Calculate the cost of riding a trip for the given {@code transitDuration} in seconds.
   */
  int transitCost(int transitDuration, T tripScheduledBoarded);

  /**
   * Calculate the value when arriving by transit.
   */
  int transitArrivalCost(
    int boardCost,
    int alightSlack,
    int transitDuration,
    T trip,
    int toStopIndex
  );

  /**
   * Calculate the cost of waiting, when waiting between two transit legs. The wait duration is
   * in seconds, and include board and alight slack.
   */
  int waitCost(int waitDuration);

  /**
   * Used for estimating the remaining value for a criteria at a given stop arrival. The calculated
   * value should be an optimistic estimate for the heuristics to work properly. So, to calculate
   * the generalized cost for given the {@code minTravelDuration} and {@code minNumTransfers}
   * returning the greatest value, which is guaranteed to be less than the <em>real value</em>
   * would be correct and a good choice.
   */
  int calculateRemainingMinCost(int minTravelDuration, int minNumTransfers, int fromStopIndex);

  /**
   * This method allows the cost calculator to add cost in addition to the generalized-cost of the
   * given egress itself. For example you might want to add a transfer cost to FLEX egress.
   *
   * @return any additional board or transfer cost.
   */
  int costEgress(int stopIndex, boolean egressHasRides);
}
