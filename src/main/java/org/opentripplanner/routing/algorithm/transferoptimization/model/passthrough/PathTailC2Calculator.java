package org.opentripplanner.routing.algorithm.transferoptimization.model.passthrough;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.request.PassThroughPoint;
import org.opentripplanner.raptor.path.PathBuilderLeg;
import org.opentripplanner.routing.algorithm.transferoptimization.model.OptimizedPathTail;
import org.opentripplanner.routing.algorithm.transferoptimization.model.TripToTripTransfer;

class PathTailC2Calculator<T extends RaptorTripSchedule> {

  private final List<List<TripToTripTransfer<T>>> possibleTransfers;
  private final List<PassThroughPoint> passThroughPoints;

  /**
   * @param possibleTransfers List all possible transfers for the whole journey
   * @param passThroughPoints Pass-through points for the search
   */
  public PathTailC2Calculator(
    List<List<TripToTripTransfer<T>>> possibleTransfers,
    List<PassThroughPoint> passThroughPoints
  ) {
    this.possibleTransfers = possibleTransfers;
    this.passThroughPoints = passThroughPoints;
  }

  /**
   * Loop through all possible boarding positions in OptimizedPathTail and calculate potential c2
   *  value given position.
   *
   * @param tail Current OptimizedPathTail
   * @return c2 value for the earliest possible boarding position.
   */
  int calculateC2(OptimizedPathTail<T> tail) {
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
    if (c2 == 0) {
      return false;
    }
    var point = passThroughPoints.get(c2 - 1);
    return point != null && point.asBitSet().get(stopIndex);
  }
}
