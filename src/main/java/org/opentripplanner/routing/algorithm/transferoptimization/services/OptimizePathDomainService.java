package org.opentripplanner.routing.algorithm.transferoptimization.services;

import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.api.path.RaptorStopNameResolver;
import org.opentripplanner.raptor.api.path.TransferPathLeg;
import org.opentripplanner.raptor.api.path.TransitPathLeg;
import org.opentripplanner.raptor.api.request.PassThroughPoint;
import org.opentripplanner.raptor.path.PathBuilderLeg;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;
import org.opentripplanner.raptor.spi.RaptorSlackProvider;
import org.opentripplanner.routing.algorithm.transferoptimization.api.OptimizedPath;
import org.opentripplanner.routing.algorithm.transferoptimization.model.MinCostFilterChain;
import org.opentripplanner.routing.algorithm.transferoptimization.model.OptimizeTransfersCostAndC2FilterChain;
import org.opentripplanner.routing.algorithm.transferoptimization.model.OptimizeTransfersFilterChain;
import org.opentripplanner.routing.algorithm.transferoptimization.model.OptimizedPathTail;
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
  private final List<PassThroughPoint> passThroughPoints;

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
    RaptorStopNameResolver stopNameTranslator,
    // TODO: 2023-09-01 Here I' including PassThroughPoints because I need a way to determine
    //  which filter we should use. I don't like this approach and would like to make it more generic
    //  but I do not know how to solve it because c2 filter depends on possibleTransfers that are generated
    //  in this class.
    List<PassThroughPoint> passThroughPoints
  ) {
    this.transferGenerator = transferGenerator;
    this.costCalculator = costCalculator;
    this.slackProvider = slackProvider;
    this.waitTimeCostCalculator = waitTimeCostCalculator;
    this.stopBoardAlightCosts = stopBoardAlightCosts;
    this.extraStopBoardAlightCostsFactor = extraStopBoardAlightCostsFactor;
    this.minCostFilterChain = minCostFilterChain;
    this.stopNameTranslator = stopNameTranslator;
    this.passThroughPoints = passThroughPoints;
  }

  public Set<OptimizedPath<T>> findBestTransitPath(RaptorPath<T> originalPath) {
    if (!passThroughPoints.isEmpty()) {
      return findBestTransitPathWithPassThroughPoints(originalPath, passThroughPoints);
    } else {
      return findBestTransitPathWithMinCost(originalPath);
    }
  }

  private List<List<TripToTripTransfer<T>>> findPossibleTransfers(RaptorPath<T> originalPath) {
    List<TransitPathLeg<T>> transitLegs = originalPath.transitLegs().collect(Collectors.toList());

    // Find all possible transfers between each pair of transit legs, and sort on arrival time
    return sortTransfersOnArrivalTimeInDecOrder(
      transferGenerator.findAllPossibleTransfers(transitLegs)
    );
  }

  /**
   * Create a filer chain function and find the best transfers combination for journey that also
   * includes all pass-through points
   * Algorithm starts with the last trip in the journey, then goes backwards looping through all
   * possible transfers for each transit leg.
   * For each possible transfer stop position C2 value is calculated. Filter chain function is going
   *  to use c2 value and cost function to determine whether tail should be included or excluded from
   *  the collection.
   *
   *  Example:
   *    Let's say we have a trip with 2 transit legs and the pass-through point is (B)
   *    There are 3 possible transfer combination with first and second transit
   *
   *    Iteration 1 (initial c2 value is 1 since we have one pass-through point):
   *      (?) Transit 2 -> (Egress) | C2 = 1
   *
   *    Iteration 2 (create all possible journey combinations with transfers and calculate c2):
   *      (?) Transit 1 -> (A) Transit 2 -> (Egress) | C2 = 0 <- C2 is 0 since we will pass through (B) if we board here
   *      (?) Transit 1 -> (B) Transit 2 -> (Egress) | C2 = 0
   *      (?) Transit 1 -> (C) Transit 2 -> (Egress) | C2 = 1 <- C2 is 1 since we will not pass through (B) if we board here
   *
   *    Iteration 3 (insert access and filter out all combinations where c2 != 0)
   *      (Access) -> Transit 1 -> (B) Transit 2 -> (Egress) | C2 = 0
   *      (Access) -> Transit 1 -> (C) Transit 2 -> (Egress) | C2 = 0
   *
   *  Then we're going to use minCostFilterChain to determine which of those 2 possibilities is better
   */
  private Set<OptimizedPath<T>> findBestTransitPathWithPassThroughPoints(
    RaptorPath<T> originalPath,
    List<PassThroughPoint> passThroughPoints
  ) {
    var possibleTransfers = findPossibleTransfers(originalPath);
    List<TransitPathLeg<T>> transitLegs = originalPath.transitLegs().collect(Collectors.toList());
    var filter = createC2FilterChain(minCostFilterChain, possibleTransfers, passThroughPoints);

    // We need to filter out all path without passThrough points
    return findBestTransferOption(originalPath, transitLegs, possibleTransfers, filter)
      .stream()
      // TODO: 2023-09-01 here we need to do one more filter to exclude all invalid paths
      //  (if a path is missing any pass-through points)
      //  Ideally this should be done "inside" the algorithm but I'm not sure how to do it
      .filter(tail -> calculateC2(tail, possibleTransfers, passThroughPoints) == 0)
      .map(OptimizedPathTail::build)
      .collect(toSet());
  }

  private Set<OptimizedPath<T>> findBestTransitPathWithMinCost(RaptorPath<T> originalPath) {
    var possibleTransfers = findPossibleTransfers(originalPath);

    List<TransitPathLeg<T>> transitLegs = originalPath.transitLegs().collect(Collectors.toList());

    // Combine transit legs and transfers
    return findBestTransferOption(originalPath, transitLegs, possibleTransfers, minCostFilterChain)
      .stream()
      .map(OptimizedPathTail::build)
      .collect(toSet());
  }

  private OptimizeTransfersFilterChain<OptimizedPathTail<T>> createC2FilterChain(
    MinCostFilterChain<OptimizedPathTail<T>> minCostFilterChain,
    List<List<TripToTripTransfer<T>>> possibleTransfers,
    List<PassThroughPoint> passThroughPoints
  ) {
    return new OptimizeTransfersCostAndC2FilterChain<>(
      minCostFilterChain,
      tail -> this.calculateC2(tail, possibleTransfers, passThroughPoints)
    );
  }

  // TODO: 2023-09-01 I don't really like that this method is here.
  //  but since we need a list of possible transfers between paths it is hard to move it
  //  some else in the code.
  /**
   * Loop through all possible boarding positions in OptimizedPathTail and calculate potential c2
   *  value given position.
   *
   * @param tail Current OptimizedPathTail
   * @param possibleTransfers List all possible transfers for the whole journey
   * @param passThroughPoints Pass-through points for the search
   * @return c2 value for the earliest possible boarding position.
   */
  private int calculateC2(
    OptimizedPathTail<T> tail,
    List<List<TripToTripTransfer<T>>> possibleTransfers,
    List<PassThroughPoint> passThroughPoints
  ) {
    var legs = new ArrayList<>(tail.legsAsStream().toList());

    var transits = legs.stream().filter(PathBuilderLeg::isTransit).toList();

    int c2;
    if (transits.size() == 1) {
      // That's the last transit in the journey we are checking. There can't be any via stops yet.
      // This should be via.size
      // TODO: 2023-09-01 Here we need to know how many pass through points we have so that we can
      //  set initial C2 value. That's why I added size() method to PassThroughPoints
      c2 = passThroughPoints.size();
    } else {
      // Check c2 value on board position from previous transit leg.
      var previousTransit = transits.get(1);
      c2 = previousTransit.c2ForStopPosition(previousTransit.fromStopPos());
    }

    var transitLeg = transits.get(0);
    int fromStopPosition;
    if (possibleTransfers.size() + 1 == transits.size()) {
      // Here we reached the first transit in the journey.
      //  We do not have to check possible transfers.
      fromStopPosition = transitLeg.fromStopPos();
    } else {
      // This should be the earliest possible board stop position for a leg.
      //  We can verify that with transfers.
      fromStopPosition =
        possibleTransfers
          .get(possibleTransfers.size() - transits.size())
          .stream()
          .map(t -> t.to().stopPosition())
          .reduce((first, second) -> first < second ? first : second)
          .get();
    }

    // TODO PT: 2023-09-01 here we are modifying existing transit legs and setting new c2 values on them.
    //  The value is gonna persist into the next loop so that we know what c2 value we had
    //  on previous (next) leg
    //  This is kinda anti-pattern. But I do not know how to solve it in other way without rewriting
    //  the whole filter logic.
    // We already visited all via stops.
    //  Don't have to check anything. Just set on all stop positions.
    if (c2 == 0) {
      for (int pos = transitLeg.toStopPos(); pos >= fromStopPosition; pos--) {
        transitLeg.setC2OnStopPosition(pos, 0);
      }
    } else {
      // Loop through all possible boarding position and calculate potential c2 value
      for (int pos = transitLeg.toStopPos(); pos >= fromStopPosition; pos--) {
        var stopIndex = transitLeg.trip().pattern().stopIndex(pos);
        // TODO PT: 2023-09-01 We need to check here whether point is a pass through for a given
        //  c2 value. That's why I need to include this method in PassThroughPoint class.
        //  This might not be the best solution. What are the other options we have?
        if (passThroughPoints.get(c2 - 1).asBitSet().get(stopIndex)) {
          c2--;
        }

        transitLeg.setC2OnStopPosition(pos, c2);
      }
    }

    return transitLeg.c2ForStopPosition(fromStopPosition);
  }

  private static <T> T last(List<T> list) {
    return list.get(list.size() - 1);
  }

  private Set<OptimizedPathTail<T>> findBestTransferOption(
    RaptorPath<T> originalPath,
    List<TransitPathLeg<T>> originalTransitLegs,
    List<List<TripToTripTransfer<T>>> possibleTransfers,
    OptimizeTransfersFilterChain<OptimizedPathTail<T>> filterChain
  ) {
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
      var tailSelector = new TransitPathLegSelector<>(filterChain, tails);

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
      new TransitPathLegSelector<>(filterChain, tails).next(originalPath.accessLeg().toTime());

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
