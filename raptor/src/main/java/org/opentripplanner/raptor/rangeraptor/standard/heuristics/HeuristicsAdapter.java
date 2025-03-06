package org.opentripplanner.raptor.rangeraptor.standard.heuristics;

import gnu.trove.map.TIntObjectMap;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntUnaryOperator;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.rangeraptor.internalapi.HeuristicAtStop;
import org.opentripplanner.raptor.rangeraptor.internalapi.Heuristics;
import org.opentripplanner.raptor.rangeraptor.internalapi.SingleCriteriaStopArrivals;
import org.opentripplanner.raptor.rangeraptor.transit.EgressPaths;
import org.opentripplanner.raptor.rangeraptor.transit.RaptorTransitCalculator;
import org.opentripplanner.raptor.rangeraptor.transit.TransitCalculator;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;
import org.opentripplanner.utils.lang.IntUtils;
import org.opentripplanner.utils.time.TimeUtils;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * The responsibility of this class is to play the {@link Heuristics} role. It wrap the internal
 * state, and transform the internal model to provide the needed functionality.
 */
public class HeuristicsAdapter implements Heuristics {

  private static final int NOT_SET = Integer.MAX_VALUE;
  private final SingleCriteriaStopArrivals bestOverallTimes;
  private final SingleCriteriaStopArrivals bestNumOfTransfers;
  private final TIntObjectMap<List<RaptorAccessEgress>> egressPaths;
  private final TransitCalculator<?> calculator;
  private final RaptorCostCalculator<?> costCalculator;
  private final int nStops;
  private final int originDepartureTime;
  private final AggregatedResults aggregatedResults;

  public HeuristicsAdapter(
    int nStops,
    EgressPaths egressPaths,
    RaptorTransitCalculator<?> calculator,
    RaptorCostCalculator<?> costCalculator,
    SingleCriteriaStopArrivals bestOverallTimes,
    SingleCriteriaStopArrivals bestTransitTimes,
    SingleCriteriaStopArrivals bestNTransfers
  ) {
    this.nStops = nStops;
    this.egressPaths = egressPaths.byStop();
    this.calculator = calculator;
    this.costCalculator = costCalculator;
    this.bestOverallTimes = bestOverallTimes;
    this.bestNumOfTransfers = bestNTransfers;
    this.originDepartureTime = calculator.minIterationDepartureTime();
    this.aggregatedResults = AggregatedResults.create(
      calculator,
      originDepartureTime,
      bestOverallTimes,
      bestTransitTimes,
      bestNTransfers,
      this.egressPaths
    );
  }

  @Override
  public int[] bestTravelDurationToIntArray(int unreached) {
    return toIntArray(size(), unreached, this::bestTravelDuration);
  }

  @Override
  public int[] bestNumOfTransfersToIntArray(int unreached) {
    return toIntArray(size(), unreached, this::bestNumOfTransfers);
  }

  @Override
  public int[] bestGeneralizedCostToIntArray(int unreached) {
    return toIntArray(size(), unreached, this::bestGeneralizedCost);
  }

  @Override
  public HeuristicAtStop createHeuristicAtStop(int stop) {
    return reached(stop)
      ? new HeuristicAtStop(
        bestTravelDuration(stop),
        bestNumOfTransfers(stop),
        bestGeneralizedCost(stop)
      )
      : HeuristicAtStop.UNREACHED;
  }

  @Override
  public int size() {
    return nStops;
  }

  @Override
  public int bestOverallJourneyTravelDuration() {
    return aggregatedResults.minJourneyTravelDuration();
  }

  @Override
  public int bestOverallJourneyNumOfTransfers() {
    return aggregatedResults.minJourneyNumOfTransfers();
  }

  @Override
  public int minWaitTimeForJourneysReachingDestination() {
    return (
      Math.abs(aggregatedResults.earliestArrivalTime() - originDepartureTime) -
      aggregatedResults.minJourneyTravelDuration()
    );
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(Heuristics.class)
      .addServiceTime("originDepartureTime(last iteration)", originDepartureTime)
      .addObj("aggregatedResults", aggregatedResults)
      .addCol(
        "egress stops reached",
        Arrays.stream(egressPaths.keys())
          .filter(bestOverallTimes::isReached)
          .mapToObj(
            s -> "[" + s + " " + TimeUtils.timeToStrCompact(bestOverallTimes.value(s)) + "]"
          )
          .limit(20)
          .toList()
      )
      .toString();
  }

  private boolean reached(int stop) {
    return bestOverallTimes.isReached(stop);
  }

  private int bestTravelDuration(int stop) {
    return calculator.duration(originDepartureTime, bestOverallTimes.value(stop));
  }

  private int bestNumOfTransfers(int stop) {
    return bestNumOfTransfers.value(stop);
  }

  private int bestGeneralizedCost(int stop) {
    return (
      costCalculator.calculateRemainingMinCost(
        bestTravelDuration(stop),
        bestNumOfTransfers(stop),
        stop
      )
    );
  }

  /**
   * Convert one of heuristics to an int array.
   */
  private int[] toIntArray(int size, int unreached, IntUnaryOperator supplier) {
    int[] a = IntUtils.intArray(size, unreached);
    for (int i = 0; i < a.length; i++) {
      if (reached(i)) {
        a[i] = supplier.applyAsInt(i);
      }
    }
    return a;
  }

  private record AggregatedResults(
    int minJourneyTravelDuration,
    int minJourneyNumOfTransfers,
    int earliestArrivalTime,
    boolean reached
  ) {
    private static AggregatedResults create(
      TransitCalculator<?> calculator,
      int originDepartureTime,
      SingleCriteriaStopArrivals bestOverallTimes,
      SingleCriteriaStopArrivals bestTransitTimes,
      SingleCriteriaStopArrivals bestNumOfTransfers,
      TIntObjectMap<List<RaptorAccessEgress>> egressPaths
    ) {
      int bestJourneyTravelDuration = NOT_SET;
      int bestJourneyNumOfTransfers = NOT_SET;
      int bestArrivalTime = NOT_SET;

      for (int stop : egressPaths.keys()) {
        var list = egressPaths.get(stop);
        boolean stopReached = bestOverallTimes.isReached(stop);
        boolean stopReachedByTransit = bestTransitTimes.isReached(stop);

        if (stopReached) {
          int durationExEgress = calculator.duration(
            originDepartureTime,
            bestOverallTimes.value(stop)
          );

          for (RaptorAccessEgress it : list) {
            // Prevent transfer(walking) and the egress which start with walking
            if (!(it.stopReachedOnBoard() || stopReachedByTransit)) {
              continue;
            }

            int d = durationExEgress + it.durationInSeconds();
            bestJourneyTravelDuration = Math.min(bestJourneyTravelDuration, d);

            int n = bestNumOfTransfers.value(it.stop());
            bestJourneyNumOfTransfers = Math.min(bestJourneyNumOfTransfers, n);

            int eat = bestOverallTimes.value(it.stop()) + it.durationInSeconds();
            bestArrivalTime = Math.min(bestArrivalTime, eat);
          }
        }
      }
      return new AggregatedResults(
        bestJourneyTravelDuration,
        bestJourneyNumOfTransfers,
        bestArrivalTime,
        bestArrivalTime != NOT_SET
      );
    }
  }
}
