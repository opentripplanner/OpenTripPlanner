package org.opentripplanner.routing.algorithm.transferoptimization.services;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.request.PassThroughPoint;
import org.opentripplanner.raptor.path.PathBuilderLeg;
import org.opentripplanner.routing.algorithm.transferoptimization.model.FilterFactory;
import org.opentripplanner.routing.algorithm.transferoptimization.model.MinCostFilterChain;
import org.opentripplanner.routing.algorithm.transferoptimization.model.OptimizeTransfersCostAndC2FilterChain;
import org.opentripplanner.routing.algorithm.transferoptimization.model.OptimizeTransfersFilterChain;
import org.opentripplanner.routing.algorithm.transferoptimization.model.OptimizedPathTail;
import org.opentripplanner.routing.algorithm.transferoptimization.model.TripToTripTransfer;

public class OptimizeTransfersCostAndC2FilterFactory<T extends RaptorTripSchedule>
  implements FilterFactory<T> {

  private final boolean transferPriority;
  private final boolean optimizeWaitTime;
  private final List<PassThroughPoint> passThroughPoints;

  public OptimizeTransfersCostAndC2FilterFactory(
    boolean transferPriority,
    boolean optimizeWaitTime,
    List<PassThroughPoint> passThroughPoints
  ) {
    this.transferPriority = transferPriority;
    this.optimizeWaitTime = optimizeWaitTime;
    this.passThroughPoints = passThroughPoints;
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
  @Override
  public OptimizeTransfersFilterChain<OptimizedPathTail<T>> createFilter(
    List<List<TripToTripTransfer<T>>> possibleTransfers
  ) {
    return new OptimizeTransfersCostAndC2FilterChain<>(
      (MinCostFilterChain<OptimizedPathTail<T>>) new TransferOptimizedFilterFactory<T>(
        transferPriority,
        optimizeWaitTime
      )
        .createFilter(possibleTransfers),
      tail -> this.calculateC2(tail, possibleTransfers, passThroughPoints)
    );
  }

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

    // TODO: 2023-09-01 here we are modifying existing transit legs and setting new c2 values on them.
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
        // TODO: 2023-09-01 We need to check here whether point is a pass through for a given
        //  c2 value. That's why I need to include this method in PassThroughPoint class.
        //  This might not be the best solution. What are the other options we have?
        if (isPassThroughPoint(passThroughPoints, c2, stopIndex)) {
          c2--;
        }

        transitLeg.setC2OnStopPosition(pos, c2);
      }
    }

    return transitLeg.c2ForStopPosition(fromStopPosition);
  }

  private boolean isPassThroughPoint(
    List<PassThroughPoint> passThroughPoints,
    int c2,
    int stopIndex
  ) {
    var point = passThroughPoints.get(c2 - 1);
    return point != null && point.asBitSet().get(stopIndex);
  }
}
