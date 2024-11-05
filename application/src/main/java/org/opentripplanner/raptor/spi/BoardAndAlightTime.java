package org.opentripplanner.raptor.spi;

import java.util.Objects;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.utils.time.DurationUtils;
import org.opentripplanner.utils.time.TimeUtils;

/**
 * Board and alight time tuple value object.
 */
public class BoardAndAlightTime {

  private final RaptorTripSchedule trip;
  private final int boardStopPos;
  private final int alightStopPos;

  public BoardAndAlightTime(RaptorTripSchedule trip, int boardStopPos, int alightStopPos) {
    this.trip = trip;
    this.boardStopPos = boardStopPos;
    this.alightStopPos = alightStopPos;
  }

  public static BoardAndAlightTime create(
    RaptorTripSchedule trip,
    int boardStop,
    int boardTime,
    int alightStop,
    int alightTime
  ) {
    return new BoardAndAlightTime(
      trip,
      trip.findDepartureStopPosition(boardTime, boardStop),
      trip.findArrivalStopPosition(alightTime, alightStop)
    );
  }

  public int boardTime() {
    return trip.departure(boardStopPos);
  }

  public int alightTime() {
    return trip.arrival(alightStopPos);
  }

  public int boardStopPos() {
    return boardStopPos;
  }

  public int alightStopPos() {
    return alightStopPos;
  }

  @Override
  public int hashCode() {
    return Objects.hash(boardStopPos, alightStopPos);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BoardAndAlightTime that = (BoardAndAlightTime) o;
    return boardStopPos == that.boardStopPos && alightStopPos == that.alightStopPos;
  }

  @Override
  public String toString() {
    return (
      "[" +
      trip.pattern().stopIndex(boardStopPos) +
      " ~ " +
      TimeUtils.timeToStrCompact(boardTime()) +
      " " +
      TimeUtils.timeToStrCompact(alightTime()) +
      "(" +
      DurationUtils.durationToStr(alightTime() - boardTime()) +
      ") ~ " +
      trip.pattern().stopIndex(alightStopPos) +
      "]"
    );
  }
}
