package org.opentripplanner.raptor._data.transit;

import static org.opentripplanner.raptor.api.model.RaptorConstants.NOT_FOUND;
import static org.opentripplanner.raptor.api.model.RaptorConstants.TIME_NOT_SET;

import java.util.List;
import org.opentripplanner.raptor.api.model.RaptorTransferConstraint;
import org.opentripplanner.raptor.api.model.SearchDirection;
import org.opentripplanner.raptor.spi.RaptorBoardOrAlightEvent;
import org.opentripplanner.raptor.spi.RaptorTripScheduleSearch;
import org.opentripplanner.utils.tostring.ToStringBuilder;

class TestTripScheduleSearch
  implements
    RaptorTripScheduleSearch<TestTripSchedule>, RaptorBoardOrAlightEvent<TestTripSchedule> {

  private final List<TestTripSchedule> trips;
  private final SearchDirection direction;

  private int tripIndex;
  private int stopPositionInPattern;
  /**
   * earliest-board-time in forward search, and latest-arrival-time for reverse search
   */
  private int timeLimit;
  private int time;

  public TestTripScheduleSearch(SearchDirection direction, List<TestTripSchedule> trips) {
    this.trips = trips;
    this.direction = direction;
    this.tripIndex = NOT_FOUND;
    this.time = TIME_NOT_SET;
  }

  @Override
  public RaptorBoardOrAlightEvent<TestTripSchedule> search(
    int earliestBoardTime,
    int stopPositionInPattern,
    int tripIndexLimit
  ) {
    this.tripIndex = NOT_FOUND;
    this.stopPositionInPattern = stopPositionInPattern;
    this.timeLimit = earliestBoardTime;

    return direction.isForward() ? searchForward(tripIndexLimit) : searchInReverse(tripIndexLimit);
  }

  private RaptorBoardOrAlightEvent<TestTripSchedule> searchForward(int tripIndexLimit) {
    final int end = tripIndexLimit == UNBOUNDED_TRIP_INDEX ? trips.size() - 1 : tripIndexLimit - 1;

    for (int i = 0; i <= end; ++i) {
      int departureTime = trips.get(i).departure(stopPositionInPattern);
      if (timeLimit <= departureTime) {
        this.time = departureTime;
        this.tripIndex = i;
        return this;
      }
    }
    return RaptorBoardOrAlightEvent.empty(timeLimit);
  }

  private RaptorBoardOrAlightEvent<TestTripSchedule> searchInReverse(int tripIndexLimit) {
    final int end = tripIndexLimit == UNBOUNDED_TRIP_INDEX ? 0 : tripIndexLimit + 1;

    for (int i = trips.size() - 1; i >= end; --i) {
      int arrivalTime = trips.get(i).arrival(stopPositionInPattern);
      if (timeLimit >= arrivalTime) {
        this.time = arrivalTime;
        this.tripIndex = i;
        return this;
      }
    }
    return RaptorBoardOrAlightEvent.empty(timeLimit);
  }

  @Override
  public int tripIndex() {
    return tripIndex;
  }

  @Override
  public TestTripSchedule trip() {
    return trips.get(tripIndex);
  }

  @Override
  public int stopPositionInPattern() {
    return stopPositionInPattern;
  }

  @Override
  public int time() {
    return time;
  }

  @Override
  public int earliestBoardTime() {
    return timeLimit;
  }

  @Override
  public RaptorTransferConstraint transferConstraint() {
    return RaptorTransferConstraint.REGULAR_TRANSFER;
  }

  @Override
  public boolean empty() {
    return tripIndex == NOT_FOUND;
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(TestTripScheduleSearch.class)
      .addBoolIfTrue("REVERSE", direction.isInReverse())
      .addNum("tripIndex", tripIndex, NOT_FOUND)
      .addNum("stopPos", stopPositionInPattern)
      .addServiceTime("timeLimit", timeLimit)
      .addServiceTime("time", time, TIME_NOT_SET)
      .toString();
  }
}
