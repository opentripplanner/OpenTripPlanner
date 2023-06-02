package org.opentripplanner.routing.algorithm.transferoptimization.services;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.api.path.RaptorStopNameResolver;
import org.opentripplanner.raptor.api.path.TransferPathLeg;
import org.opentripplanner.raptor.api.path.TransitPathLeg;
import org.opentripplanner.raptor.path.PathBuilderLeg;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;
import org.opentripplanner.raptor.spi.RaptorSlackProvider;
import org.opentripplanner.routing.algorithm.transferoptimization.api.OptimizedPath;
import org.opentripplanner.routing.algorithm.transferoptimization.model.MinCostFilterChain;
import org.opentripplanner.routing.algorithm.transferoptimization.model.OptimizedPathTail;
import org.opentripplanner.routing.algorithm.transferoptimization.model.OptimizedTransferCostAndC2FilterChain;
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
 * construction we get close to O(N*M) - which is acceptable.
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
  private final MinCostFilterChain<OptimizedPathTail<T>> minCostFilterChain;
  private final RaptorStopNameResolver stopNameTranslator;

  @Nullable
  private final TransferWaitTimeCostCalculator waitTimeCostCalculator;

  @Nullable
  private final int[] stopBoardAlightCosts;

  private final double extraStopBoardAlightCostsFactor;

  public OptimizePathDomainService(
    TransferGenerator<T> transferGenerator,
    RaptorCostCalculator<T> costCalculator,
    RaptorSlackProvider slackProvider,
    @Nullable TransferWaitTimeCostCalculator waitTimeCostCalculator,
    int[] stopBoardAlightCosts,
    double extraStopBoardAlightCostsFactor,
    MinCostFilterChain<OptimizedPathTail<T>> minCostFilterChain,
    RaptorStopNameResolver stopNameTranslator
  ) {
    this.transferGenerator = transferGenerator;
    this.costCalculator = costCalculator;
    this.slackProvider = slackProvider;
    this.waitTimeCostCalculator = waitTimeCostCalculator;
    this.stopBoardAlightCosts = stopBoardAlightCosts;
    this.extraStopBoardAlightCostsFactor = extraStopBoardAlightCostsFactor;
    this.minCostFilterChain = minCostFilterChain;
    this.stopNameTranslator = stopNameTranslator;
  }

  public Set<OptimizedPath<T>> findBestTransitPath(RaptorPath<T> originalPath, List<Set<Integer>> viaIndexes) {
    List<TransitPathLeg<T>> transitLegs = originalPath.transitLegs().collect(Collectors.toList());

    // Find all possible transfers between each pair of transit legs, and sort on arrival time
    var possibleTransfers = sortTransfersOnArrivalTimeInDecOrder(
      transferGenerator.findAllPossibleTransfers(transitLegs)
    );

    // Combine transit legs and transfers
    var tails = findBestTransferOption(originalPath, transitLegs, possibleTransfers, viaIndexes);

    return tails.stream().map(OptimizedPathTail::build).collect(Collectors.toSet());
  }

  private static <T> T last(List<T> list) {
    return list.get(list.size() - 1);
  }

  private Set<OptimizedPathTail<T>> findBestTransferOption(
    RaptorPath<T> originalPath,
    List<TransitPathLeg<T>> originalTransitLegs,
    List<List<TripToTripTransfer<T>>> possibleTransfers,
    List<Set<Integer>> viaIndexes
  ) {

    var c2FilterChain = new OptimizedTransferCostAndC2FilterChain<>(
      minCostFilterChain,
      tail -> this.calculateC2(tail, possibleTransfers, viaIndexes)
    );

    final int iterationDepartureTime = originalPath.rangeRaptorIterationDepartureTime();
    // Create a set of tails with the last transit leg in it (one element)
    Set<OptimizedPathTail<T>> tails = Set.of(
      new OptimizedPathTail<>(
        slackProvider,
        costCalculator,
        iterationDepartureTime,
        waitTimeCostCalculator,
        stopBoardAlightCosts,
        extraStopBoardAlightCostsFactor,
        stopNameTranslator
      )
        .addTransitTail(last(originalTransitLegs))
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
      var tailSelector = new TransitPathLegSelector<>(c2FilterChain, tails);

      // Reset the result set to an empty set
      tails = new HashSet<>();

      for (TripToTripTransfer<T> tx : transfers) {
        // Skip transfers happening before earliest possible board time
        if (tx.from().time() <= earliestDepartureTimeFromLeg) {
          continue;
        }

        // Find the best tails that are safe to board with respect to the arrival
        var candidateTails = tailSelector.next(tx.to().time());

        for (OptimizedPathTail<T> tail : candidateTails) {
          // Tail can be used with current transfer
          if (tail != null) {
            tails.add(createNewTransitLegTail(originalFromTransitLeg, tx, tail));
          }
        }
      }
    }

    // Filter tails one final time
    tails =
      new TransitPathLegSelector<>(c2FilterChain, tails)
        .next(originalPath.accessLeg().toTime());

    // Insert the access leg and the following transfer
    insertAccess(originalPath, tails);

    Set<OptimizedPathTail<T>> result = new HashSet<>();

    // Here we are filtering out all tails which are missing via points
    for (var tail : tails) {
      var c2 = calculateC2(tail, possibleTransfers, viaIndexes);

      if (c2 == 0) {
        result.add(tail);
      }
    }

    return result;
  }

  public int calculateC2(OptimizedPathTail<T> tail,
                         List<List<TripToTripTransfer<T>>> possibleTransfers,
                         List<Set<Integer>> via) {

    var legs = new ArrayList<>(tail.legsAsStream().toList());

    var transits = legs.stream().filter(PathBuilderLeg::isTransit).toList();

    int c2;
    if (transits.size() == 1) {
      // That's the first journey we are checking. There can't be any via stops yet.
      // This should be via.size
      c2 = via.size();
    } else {
      // Check c2 value on board position from previous transit leg.
      var previousTransit = transits.get(1);
      c2 = previousTransit.c2PerStopPosition()[previousTransit.fromStopPos()];
    }

    var transitLeg = transits.get(0);
    // This should be the earliest possible board stop position for a leg
    //  We can verify that with transfers.
    // Or if previous leg is access then we know what position it is
    int fromStopPosition;
    if (possibleTransfers.size() + 1 == transits.size()) {
      fromStopPosition = transitLeg.fromStopPos();
    } else {
      fromStopPosition = possibleTransfers.get(possibleTransfers.size() - transits.size()).stream()
        .map(t -> t.to().stopPosition())
        .reduce((first, second) -> first < second ? first: second)
        .get();
    }

    for (int pos = transitLeg.toStopPos(); pos >= fromStopPosition; pos--) {
      // We already visited all via stops
      if (c2 == 0) {
        transitLeg.c2PerStopPosition()[pos] = c2;
        continue;
      }
      var indexes = via.get(c2 - 1);

      var pattern = transitLeg.trip().pattern();
      if (indexes.contains(pattern.stopIndex(pos))) {
        c2--;
      }

      transitLeg.c2PerStopPosition()[pos] = c2;
    }

    // TODO: 2023-06-02 via pass through: this is debug output to check whether everything works fine
//    var path = new ArrayList<String>();
//
//    new ArrayList<>(tail.legsAsStream().toList()).forEach(leg -> {
//      if (leg.isEgress()) {
//        path.add("Egress");
//      } else if (leg.isAccess()) {
//        path.add("Access");
//      } else if (leg.isTransfer()) {
//        path.add("Transfer");
//      } else {
//        path.add("Transit");
//      }
//    });
//
//    if (path.get(0).equals("Access")) {
//      System.out.println("=======================================");
//      System.out.println(String.join(" - ", path));
//
//      for (int pos = fromStopPosition; pos <= transitLeg.toStopPos(); pos++) {
//        var pattern = transitLeg.trip().pattern();
//        System.out.println(stopNameTranslator.apply(pattern.stopIndex(pos)) + " - " + transitLeg.getC2PerStopPosition()[pos]);
//      }
//
//      System.out.println("-");
//      for (int i = 1; i < transits.size(); i++) {
//        var transit = transits.get(i);
//        for (int pos = transit.fromStopPos(); pos <= transit.toStopPos(); pos++) {
//          var pattern = transit.trip().pattern();
//          System.out.println(stopNameTranslator.apply(pattern.stopIndex(pos)) + " - " + transit.getC2PerStopPosition()[pos]);
//        }
//        System.out.println("-");
//      }
//    }

    return transitLeg.c2PerStopPosition()[fromStopPosition];
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

  private List<List<TripToTripTransfer<T>>> sortTransfersOnArrivalTimeInDecOrder(
    List<List<TripToTripTransfer<T>>> transfers
  ) {
    return transfers
      .stream()
      .map(it ->
        it
          .stream()
          .sorted(Comparator.comparingInt(l -> -l.to().time()))
          .collect(Collectors.toList())
      )
      .collect(Collectors.toList());
  }
}
