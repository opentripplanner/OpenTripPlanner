package org.opentripplanner.raptor.rangeraptor.standard;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import java.util.Arrays;
import java.util.Collection;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.rangeraptor.internalapi.WorkerLifeCycle;
import org.opentripplanner.raptor.rangeraptor.transit.RaptorTransitCalculator;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;

/**
 * Early Pruning optimization for Standard Range Raptor transfer relaxation.
 * <p>
 * This optimization breaks the transfer loop early when the current arrival time exceeds the best
 * known destination arrival time. It requires transfers to be sorted by increasing duration, so
 * that once a transfer exceeds the bound, all subsequent transfers will too. Transfers with equal
 * duration are acceptable — they produce the same arrival time and are all pruned together.
 * <p>
 * The optimization is only applied to Standard RAPTOR (not Multi-Criteria). In MC-Raptor, multiple
 * Pareto-optimal arrivals at each stop must be explored; pruning based on a single best destination
 * time would discard arrivals that are suboptimal in time but optimal in cost.
 * <p>
 * Two bounds are maintained for correctness with Range Raptor:
 * <ol>
 *   <li>A per-round bound ({@code bestDestArrivalByRound}) tracking the best destination arrival
 *   across all Range Raptor iterations for the current round. This ensures that a fast path from an
 *   earlier iteration (later departure) does not incorrectly prune a path in the current iteration,
 *   since the Pareto set includes both departure time and arrival time.</li>
 *   <li>A within-iteration bound ({@code bestDestCurrentIteration}) tracking the best destination
 *   arrival seen in earlier rounds of the current iteration. Within a fixed departure-time
 *   iteration, rounds are processed sequentially, so this bound is safe to apply.</li>
 * </ol>
 * See: Rohovyi, Abuaisha, Walsh — "Early Pruning for Public Transport Routing", WCTR 2026
 * (<a href="https://arxiv.org/abs/2603.12592">arxiv.org/abs/2603.12592</a>).
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class StdTransferEarlyPruning<T extends RaptorTripSchedule> {

  /** Returned by the egress map for stops that are not egress stops. Durations are non-negative. */
  private static final int NOT_EGRESS = -1;

  /**
   * Minimum egress duration per egress stop. The map is sparse — it holds only egress stops, so its
   * memory scales with the number of egress paths rather than the number of stops in the graph,
   * which matters for large transit networks (100 000+ stops). The {@code get} lookup is O(1),
   * keeping {@link #updateArrival(int, int)} cheap; that method is called on every best-time
   * improvement during both the transit and transfer phases.
   */
  private final TIntIntMap egressMinDurationByStop;
  private final int[] bestDestArrivalByRound;
  private final RaptorTransitCalculator<T> calculator;
  private int bestDestCurrentIteration;
  private int currentRound;

  public StdTransferEarlyPruning(
    Collection<RaptorAccessEgress> egressPaths,
    int nRounds,
    RaptorTransitCalculator<T> calculator,
    WorkerLifeCycle lifeCycle
  ) {
    this.egressMinDurationByStop = new TIntIntHashMap(egressPaths.size(), 0.5f, -1, NOT_EGRESS);
    for (var egress : egressPaths) {
      int stop = egress.stop();
      int duration = egress.durationInSeconds();
      int current = egressMinDurationByStop.get(stop);
      if (current == NOT_EGRESS || duration < current) {
        egressMinDurationByStop.put(stop, duration);
      }
    }
    this.calculator = calculator;
    this.bestDestArrivalByRound = new int[nRounds + 1];
    Arrays.fill(this.bestDestArrivalByRound, calculator.unreachedTime());
    this.bestDestCurrentIteration = calculator.unreachedTime();
    this.currentRound = 0;
    lifeCycle.onPrepareForNextRound(round -> this.currentRound = round);
    lifeCycle.onSetupIteration(ignore ->
      this.bestDestCurrentIteration = calculator.unreachedTime()
    );
  }

  /**
   * Notify early pruning of a new best arrival at {@code stop}. If this is an egress stop, the
   * destination arrival bounds are updated.
   */
  void updateArrival(int stop, int alightTime) {
    int egressMinDuration = egressMinDurationByStop.get(stop);
    if (egressMinDuration == NOT_EGRESS) {
      return;
    }
    int destArrival = calculator.plusDuration(alightTime, egressMinDuration);
    if (calculator.isBefore(destArrival, bestDestArrivalByRound[currentRound])) {
      bestDestArrivalByRound[currentRound] = destArrival;
    }
    if (calculator.isBefore(destArrival, bestDestCurrentIteration)) {
      bestDestCurrentIteration = destArrival;
    }
  }

  /**
   * Returns {@code true} if {@code arrivalTime} is not better than the best known destination
   * arrival time, meaning this transfer — and all subsequent longer ones — can be skipped.
   */
  boolean exceedsBound(int arrivalTime) {
    int bound = bestDestCurrentIteration;
    if (calculator.isBefore(bestDestArrivalByRound[currentRound], bound)) {
      bound = bestDestArrivalByRound[currentRound];
    }
    return !calculator.isBefore(arrivalTime, bound);
  }
}
