package org.opentripplanner.routing.algorithm.transferoptimization.services;

import static java.util.stream.Collectors.toSet;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.api.model.RaptorStopNameResolver;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.api.path.TransferPathLeg;
import org.opentripplanner.raptor.api.path.TransitPathLeg;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;
import org.opentripplanner.raptor.spi.RaptorSlackProvider;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RaptorTransitData;
import org.opentripplanner.routing.algorithm.transferoptimization.api.OptimizedPath;
import org.opentripplanner.routing.algorithm.transferoptimization.model.OptimizedPathTail;
import org.opentripplanner.routing.algorithm.transferoptimization.model.PathTailFilter;
import org.opentripplanner.routing.algorithm.transferoptimization.model.TransferWaitTimeCostCalculator;
import org.opentripplanner.routing.algorithm.transferoptimization.model.TripToTripTransfer;

/**
 * This class is responsible for generating all possible permutations of a path with respect to
 * where the transfer happens and picking the best possible path. To improve performance, the paths
 * generated are pruned during construction.
 * <p>
 * <b>Performance</b>
 * <p>
 * <pre>
 * N : number of transfers in path
 * M : number of possible transfer location for a given pair of trips
 *
 * Without any pruning the permutations have an order of O(N^M), but by filtering during the path
 * construction we get close to o(N*M) - which is acceptable.
 *
 * Example with 3 lines(trips), where the `+` indicate stop places:
 *
 * Stop:     A     B     C     D     E     F     G
 * Line 1:  + --- + --------- +
 * Line 2:        + --- + --- + --- + --- +
 * Line 3:              + --------- + --- + --- +
 *
 * In this example, all the following paths are possible from A to G:
 *
 *   A ~ L1 ~ B ~ L2 ~ C ~ L3 ~ G   (best option)
 *   A ~ L1 ~ B ~ L2 ~ E ~ L3 ~ G
 *   A ~ L1 ~ B ~ L2 ~ F ~ L3 ~ G
 *   A ~ L1 ~ D ~ L2 ~ E ~ L3 ~ G   (second best option)
 *   A ~ L1 ~ D ~ L2 ~ F ~ L3 ~ G
 *
 * However the implementation filters after finding each sub-path to generate less alternatives.
 * The construction starts from the end (tail) and works its way forward.
 *
 * 1. Find transfer between L2 and L3:
 *    1.1 When boarding L2 at D we have 2 options for the next transfer: E and F => E is best
 *    1.2 When boarding L2 at B we have 2 options for the next transfer: C and E (best of E & F) => C is best
 *    1.3 Tails found: [ L2 ~ E ~ L3 ~ G, L2 ~ C ~ L3 ~ G ]
 *
 * 2. Find transfer between L1 and L2, with 2 possible tails:
 *    1.1 Board L2 at D, only one possible tail to use: L2 ~ E ~ L3 ~ G  =>  A ~ L1 ~ D ~ L2 ~ E ~ L3 ~ G
 *    1.2 Board L2 at B, 2 possible tails, but the best is: L2 ~ C ~ L3 ~ G =>  A ~ L1 ~ B ~ L2 ~ C ~ L3 ~ G
 *    1.3 The best path is: A ~ L1 ~ B ~ L2 ~ C ~ L3 ~ G
 * </pre>
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class OptimizePathDomainService<T extends RaptorTripSchedule> {

  private final TransferGenerator<T> transferGenerator;
  private final RaptorCostCalculator<T> costCalculator;
  private final RaptorSlackProvider slackProvider;
  private final PathTailFilter<T> filter;
  private final RaptorStopNameResolver stopNameTranslator;

  @Nullable
  private final TransferWaitTimeCostCalculator waitTimeCostCalculator;

  /**
   * @see RaptorTransitData#getStopBoardAlightTransferCosts()
   */
  @Nullable
  private final int[] stopBoardAlightTransferCosts;

  private final double extraStopBoardAlightCostsFactor;

  public OptimizePathDomainService(
    TransferGenerator<T> transferGenerator,
    RaptorCostCalculator<T> costCalculator,
    RaptorSlackProvider slackProvider,
    @Nullable TransferWaitTimeCostCalculator waitTimeCostCalculator,
    @Nullable int[] stopBoardAlightTransferCosts,
    double extraStopBoardAlightCostsFactor,
    PathTailFilter<T> filter,
    RaptorStopNameResolver stopNameTranslator
  ) {
    this.transferGenerator = transferGenerator;
    this.costCalculator = costCalculator;
    this.slackProvider = slackProvider;
    this.waitTimeCostCalculator = waitTimeCostCalculator;
    this.stopBoardAlightTransferCosts = stopBoardAlightTransferCosts;
    this.extraStopBoardAlightCostsFactor = extraStopBoardAlightCostsFactor;
    this.filter = filter;
    this.stopNameTranslator = stopNameTranslator;
  }

  public Set<OptimizedPath<T>> findBestTransitPath(RaptorPath<T> originalPath) {
    List<TransitPathLeg<T>> transitLegs = originalPath.transitLegs().collect(Collectors.toList());

    // Find all possible transfers between each pair of transit legs, and sort on stop position
    var possibleTransfers = sortTransfersOnArrivalStopPosInDecOrder(
      transferGenerator.findAllPossibleTransfers(transitLegs)
    );

    // Combine transit legs and transfers
    var tails = findBestTransferOption(originalPath, transitLegs, possibleTransfers, filter);

    var filteredTails = filter.filterFinalResult(tails);

    setC2IfNotSet(originalPath, filteredTails);

    return filteredTails.stream().map(OptimizedPathTail::build).collect(toSet());
  }

  private static <T> T last(List<T> list) {
    return list.get(list.size() - 1);
  }

  private Set<OptimizedPathTail<T>> findBestTransferOption(
    RaptorPath<T> originalPath,
    List<TransitPathLeg<T>> originalTransitLegs,
    List<List<TripToTripTransfer<T>>> possibleTransfers,
    PathTailFilter<T> filter
  ) {
    final int iterationDepartureTime = originalPath.rangeRaptorIterationDepartureTime();
    // Create a set of tails with the last transit leg in it (one element)
    Set<OptimizedPathTail<T>> tails = Set.of(
      new OptimizedPathTail<>(
        slackProvider,
        costCalculator,
        iterationDepartureTime,
        waitTimeCostCalculator,
        stopBoardAlightTransferCosts,
        extraStopBoardAlightCostsFactor,
        stopNameTranslator
      ).addTransitTail(last(originalTransitLegs))
    );

    // Cache accessArrivalTime, any event before the access-arrival-time is safe to ignore
    int accessArrivalTime = originalPath.accessLeg().toTime();

    for (int i = possibleTransfers.size() - 1; i >= 0; --i) {
      // Get the list of transfers for the current index
      List<TripToTripTransfer<T>> transfers = possibleTransfers.get(i);
      TransitPathLeg<T> originalFromTransitLeg = originalTransitLegs.get(i);

      // Find the earliest possible time we can arrive and still find a matching transfer in the
      // next iteration (looking at the boarding of the current transit leg)
      int earliestDepartureTimeFromLeg = i == 0
        ? accessArrivalTime
        // The transfer with the earliest-arrival-time BEFORE the transit-leg is used to
        // prune the transfers AFTER the transit-leg. The transfers are sorted on
        // arrival-time in descending order, so the earliest-arrival-time is the
        // last element of the list of transfers.
        : last(possibleTransfers.get(i - 1)).to().time();

      // create a tailSelector for the tails produced in the last round and use it to filter them
      // based on the transfer-arrival-time and given filter
      var tailSelector = new TransitPathLegSelector<>(filter, tails);

      // Reset the result set to an empty set
      tails = new HashSet<>();

      for (TripToTripTransfer<T> tx : transfers) {
        // Skip transfers happening before the earliest possible board time
        if (tx.from().time() <= earliestDepartureTimeFromLeg) {
          continue;
        }

        // Find the best tails that are safe to board with respect to the arrival
        var candidateTails = tailSelector.next(tx.to().stopPosition());

        for (OptimizedPathTail<T> tail : candidateTails) {
          // Tail can be used with current transfer
          if (tail != null) {
            tails.add(createNewTransitLegTail(originalFromTransitLeg, tx, tail));
          }
        }
      }
    }

    // Filter tails one final time
    tails = new TransitPathLegSelector<>(filter, tails).next(
      originalPath.accessLeg().nextTransitLeg().getFromStopPosition()
    );

    // Insert the access leg and the following transfer
    insertAccess(originalPath, tails);

    return tails;
  }

  /**
   * Insert the access leg and the following transfer. The transfer can only exist if the access has
   * rides (is FLEX).
   */
  private void insertAccess(RaptorPath<T> originalPath, Set<OptimizedPathTail<T>> tails) {
    var accessLeg = originalPath.accessLeg();
    var nextLeg = accessLeg.nextLeg();
    TransferPathLeg<T> nextTransferLeg = null;
    TransitPathLeg<T> nextTransitLeg;

    if (nextLeg.isTransferLeg()) {
      nextTransferLeg = nextLeg.asTransferLeg();
      nextTransitLeg = nextTransferLeg.nextLeg().asTransitLeg();
    } else {
      nextTransitLeg = accessLeg.nextLeg().asTransitLeg();
    }

    int boardPos = nextTransitLeg.getFromStopPosition();

    for (OptimizedPathTail<T> path : tails) {
      path.head().changeBoardingPosition(boardPos);
      if (nextTransferLeg != null) {
        path.transfer(nextTransferLeg.transfer(), nextTransferLeg.toStop());
      }
      path.access(accessLeg.access());
    }
  }

  /**
   * Create a new {@link OptimizedPathTail} for the originalLeg with the given transfer,
   * earliest-departure-time and following leg (tail).
   * <p>
   * Since the previous leg is not yet known, the earliest-departure-time is used instead. For the
   * first transit leg in a path the {@code earliestDepartureTime} must be set to the correct values
   * (the access-leg-arrival-time), for all other cases it only need to be before the first possible
   * boarding.
   */
  private OptimizedPathTail<T> createNewTransitLegTail(
    TransitPathLeg<T> originalLeg,
    TripToTripTransfer<T> tx,
    OptimizedPathTail<T> tail
  ) {
    return tail.mutate().addTransitAndTransferLeg(originalLeg, tx);
  }

  private List<List<TripToTripTransfer<T>>> sortTransfersOnArrivalStopPosInDecOrder(
    List<List<TripToTripTransfer<T>>> transfers
  ) {
    return transfers
      .stream()
      .map(it ->
        it
          .stream()
          .sorted(Comparator.comparingInt(l -> -l.to().stopPosition()))
          .collect(Collectors.toList())
      )
      .collect(Collectors.toList());
  }

  /**
   * Copy over c2 value from origin to new path if the c2 value is not generated by this service.
   */
  private static <T extends RaptorTripSchedule> void setC2IfNotSet(
    RaptorPath<T> originalPath,
    Set<OptimizedPathTail<T>> filteredTails
  ) {
    if (originalPath.isC2Set()) {
      for (OptimizedPathTail<T> tail : filteredTails) {
        if (!tail.isC2Set()) {
          tail.c2(originalPath.c2());
        }
      }
    }
  }
}
