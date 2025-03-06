package org.opentripplanner.raptor.rangeraptor.transit;

import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.spi.RaptorBoardOrAlightEvent;
import org.opentripplanner.raptor.spi.RaptorTripScheduleSearch;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * This trip search will only match trips that is within the given slack of the timeLimit.
 * <p/>
 * Let say we want to board a trip and the 'earliest boarding time' is 12:10:00, and the slack is 60
 * seconds. Then all trip leaving from 12:10:00 to 12:11:00 is accepted. This is used to prevent
 * boarding trips that depart long after the Range Raptor search window. The Range Raptor algorithm
 * implemented here uses this wrapper for round 1, for all other rounds the normal {@code
 * TripScheduleBoardSearch} or {@code TripScheduleAlightSearch} is used.
 * <p/>
 * This class do not perform the trip search, but delegates this.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class TripScheduleExactMatchSearch<T extends RaptorTripSchedule>
  implements RaptorTripScheduleSearch<T> {

  private final int slack;
  private final RaptorTripScheduleSearch<T> delegate;
  private final TransitCalculator<T> calculator;

  TripScheduleExactMatchSearch(
    RaptorTripScheduleSearch<T> delegate,
    TransitCalculator<T> calculator,
    int slack
  ) {
    this.delegate = delegate;
    this.slack = slack;
    this.calculator = calculator;
  }

  @Override
  public RaptorBoardOrAlightEvent<T> search(
    int timeLimit,
    int stopPositionInPattern,
    int tripIndexLimit
  ) {
    RaptorBoardOrAlightEvent<T> result = delegate.search(
      timeLimit,
      stopPositionInPattern,
      tripIndexLimit
    );
    if (result.empty() || result.transferConstraint().isNotAllowed()) {
      return result;
    }
    return isWithinSlack(timeLimit, result.time())
      ? result
      : RaptorBoardOrAlightEvent.empty(result.earliestBoardTime());
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(TripScheduleExactMatchSearch.class)
      .addNum("slack", slack)
      .addObj("delegate", delegate)
      .toString();
  }

  private boolean isWithinSlack(int timeLimit, int time) {
    return calculator.isBefore(time, timeLimit + slack);
  }
}
