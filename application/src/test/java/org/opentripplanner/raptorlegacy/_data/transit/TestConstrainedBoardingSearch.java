package org.opentripplanner.raptorlegacy._data.transit;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.opentripplanner.model.transfer.TransferConstraint;
import org.opentripplanner.raptor.spi.RaptorBoardOrAlightEvent;
import org.opentripplanner.raptor.spi.RaptorConstrainedBoardingSearch;
import org.opentripplanner.raptor.spi.RaptorTimeTable;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * @deprecated This was earlier part of Raptor and should not be used outside the Raptor
 *             module. Use the OTP model entities instead.
 */
@Deprecated
public class TestConstrainedBoardingSearch
  implements RaptorConstrainedBoardingSearch<TestTripSchedule> {

  /** Index of guaranteed transfers by fromStopPos */
  private final TIntObjectMap<List<TestConstrainedTransfer>> transfersByFromStopPos =
    new TIntObjectHashMap<>();
  private final BitSet transfersByToStopPosExist = new BitSet();
  private final BiPredicate<Integer, Integer> timeAfterOrEqual;
  private int currentTargetStopPos;

  TestConstrainedBoardingSearch(boolean forward) {
    this.timeAfterOrEqual = forward ? (a, b) -> a >= b : (a, b) -> a <= b;
  }

  @Override
  public boolean transferExistTargetStop(int targetStopPos) {
    this.currentTargetStopPos = targetStopPos;
    return transfersByFromStopPos.containsKey(targetStopPos);
  }

  @Override
  public boolean transferExistSourceStop(int targetStopPos) {
    // This is only used to check for
    return transfersByToStopPosExist.get(targetStopPos);
  }

  @Nullable
  @Override
  public RaptorBoardOrAlightEvent<TestTripSchedule> find(
    RaptorTimeTable<TestTripSchedule> targetTimetable,
    int transferSlack,
    TestTripSchedule sourceTrip,
    int sourceStopIndex,
    int prevTransitArrivalTime,
    int earliestBoardTime
  ) {
    var list = transfersByFromStopPos.get(currentTargetStopPos);
    for (TestConstrainedTransfer tx : list) {
      var trip = tx.getSourceTrip();
      if (trip == sourceTrip) {
        int stopPos = trip.findDepartureStopPosition(prevTransitArrivalTime, sourceStopIndex);
        boolean boardAlightPossible = timeAfterOrEqual.test(tx.getTime(), prevTransitArrivalTime);
        if (tx.getSourceStopPos() == stopPos && boardAlightPossible) {
          return tx.boardingEvent(tx.isFacilitated() ? prevTransitArrivalTime : earliestBoardTime);
        }
      }
    }
    return RaptorBoardOrAlightEvent.empty(earliestBoardTime);
  }

  /**
   * Return boardings as a result for constrained transfers like a guaranteed transfer.
   */
  public List<TestConstrainedTransfer> constrainedBoardings() {
    return transfersByFromStopPos
      .valueCollection()
      .stream()
      .flatMap(Collection::stream)
      .collect(Collectors.toList());
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(TestConstrainedBoardingSearch.class)
      .addNum("currentTargetStopPos", currentTargetStopPos)
      .addObj("index", transfersByFromStopPos)
      .toString();
  }

  /**
   * The the {@code source/target} is the trips in order of the search direction (forward or
   * reverse). For reverse search it is the opposite from {@code from/to} in the result path.
   */
  void addConstraintTransfers(
    TestTripSchedule sourceTrip,
    int sourceStopPos,
    TestTripSchedule targetTrip,
    int targetTripIndex,
    int targetStopPos,
    int targetTime,
    TransferConstraint constraint
  ) {
    List<TestConstrainedTransfer> list = transfersByFromStopPos.get(targetStopPos);
    if (list == null) {
      list = new ArrayList<>();
      transfersByFromStopPos.put(targetStopPos, list);
    }
    list.add(
      new TestConstrainedTransfer(
        constraint,
        sourceTrip,
        sourceStopPos,
        targetTrip,
        targetTripIndex,
        targetStopPos,
        targetTime
      )
    );
    transfersByToStopPosExist.set(sourceStopPos);
  }

  void clear() {
    transfersByFromStopPos.clear();
    transfersByToStopPosExist.clear();
  }
}
