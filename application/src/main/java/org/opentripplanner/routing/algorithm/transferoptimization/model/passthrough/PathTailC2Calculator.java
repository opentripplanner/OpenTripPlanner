package org.opentripplanner.routing.algorithm.transferoptimization.model.passthrough;

import java.util.List;
import org.opentripplanner.raptor.api.request.RaptorViaLocation;
import org.opentripplanner.raptor.path.PathBuilderLeg;
import org.opentripplanner.routing.algorithm.transferoptimization.model.OptimizedPathTail;

class PathTailC2Calculator {

  private final List<RaptorViaLocation> viaLocations;

  PathTailC2Calculator(List<RaptorViaLocation> viaLocations) {
    this.viaLocations = viaLocations;
  }

  int calculateC2(OptimizedPathTail<?> tail) {
    return calculateAndSetC2ForLeg(tail.head()).currC2();
  }

  int calculateC2AtStopPos(OptimizedPathTail<?> tail, int fromStopPos) {
    return calculateC2ForLeg(tail.head(), fromStopPos);
  }

  private int calculateC2ForLeg(PathBuilderLeg<?> curr, int fromStopPos) {
    var ptpIter = calculateAndSetC2ForLeg(curr);
    calculateC2AtStopPos(curr, fromStopPos, ptpIter);
    return ptpIter.currC2();
  }

  private PassThroughPointsIterator calculateAndSetC2ForLeg(PathBuilderLeg<?> tail) {
    PassThroughPointsIterator ptpIter;

    var curr = findFirstLegWithC2Set(tail);

    if (curr.isEgress()) {
      ptpIter = PassThroughPointsIterator.tailIterator(viaLocations);
      curr.c2(ptpIter.currC2());
    } else {
      ptpIter = PassThroughPointsIterator.tailIterator(viaLocations, curr.c2());
    }

    while (curr != tail) {
      if (curr.isTransit()) {
        calculateC2AtStopPos(curr, curr.fromStopPos(), ptpIter);
      }
      curr = curr.prev();
      curr.c2(ptpIter.currC2());
    }
    curr.c2(ptpIter.currC2());
    return ptpIter;
  }

  /**
   * Find the first leg that has the c2 value set - starting with the given leg and ending with
   * the egress leg. If no c2 value is set in the tail, then the egress leg is returned.
   */
  private PathBuilderLeg<?> findFirstLegWithC2Set(PathBuilderLeg<?> tail) {
    while (!tail.isEgress()) {
      if (tail.isC2Set()) {
        return tail;
      }
      tail = tail.next();
    }
    return tail;
  }

  private void calculateC2AtStopPos(
    PathBuilderLeg<?> leg,
    int stopPos,
    PassThroughPointsIterator ptpIter
  ) {
    var pattern = leg.trip().pattern();
    for (int pos = leg.toStopPos(); pos >= stopPos; --pos) {
      if (ptpIter.isPassThroughPoint(pattern.stopIndex(pos))) {
        ptpIter.next();
      }
    }
  }
}
