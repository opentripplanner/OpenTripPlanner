package org.opentripplanner.routing.algorithm.raptor.transit;

import java.time.Duration;
import java.time.ZonedDateTime;
import org.opentripplanner.model.base.ToStringBuilder;
import org.opentripplanner.routing.algorithm.raptor.transit.cost.RaptorCostConverter;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TimeRestrictionWithOffset;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;

public class AccessEgress implements RaptorTransfer {

  private static final int MAX_TIME_RESTRICTION_ITERATIONS = 10;

  /**
   * "To stop" in the case of access, "from stop" in the case of egress.
   */
  private int toFromStop;

  private final int durationInSeconds;

  private final int generalizedCost;

  /**
   * This should be the last state both in the case of access and egress.
   */
  private final State lastState;

  private final ZonedDateTime startOfTime;

  public AccessEgress(
          int toFromStop,
          State lastState,
          ZonedDateTime startOfTime
  ) {
    this.toFromStop = toFromStop;
    this.durationInSeconds = (int) lastState.getElapsedTimeSeconds();
    this.generalizedCost = RaptorCostConverter.toRaptorCost(lastState.getWeight());
    this.lastState = lastState;
    this.startOfTime = startOfTime;
  }

  @Override
  public int stop() {
    return toFromStop;
  }

  @Override
  public int generalizedCost() {
    return generalizedCost;
  }

  @Override
  public int durationInSeconds() {
    return durationInSeconds;
  }

  @Override
  public int earliestDepartureTime(int requestedDepartureTime) {
    var timeRestrictions = getLastState().getTimeRestrictions();
    if (timeRestrictions.isEmpty()) {
      return requestedDepartureTime;
    }

    var time = startOfTime.plusSeconds(requestedDepartureTime)
            .toLocalDateTime();
    var iterations = 0;

    DATETIME_SEARCH:
    while (iterations < MAX_TIME_RESTRICTION_ITERATIONS) {
      for (final TimeRestrictionWithOffset timeRestriction : timeRestrictions) {
        var timeAtRestriction = time.plusSeconds(timeRestriction.getOffsetInSecondsFromStartOfSearch());
        var traversableAt = timeRestriction.getTimeRestriction()
                .earliestDepartureTime(timeAtRestriction);

        if (traversableAt.isEmpty()) {
          break DATETIME_SEARCH;
        }

        var alternateTime = traversableAt.get();
        if (!alternateTime.equals(timeAtRestriction)) {
          time = alternateTime.minusSeconds(timeRestriction.getOffsetInSecondsFromStartOfSearch());
          iterations++;
          continue DATETIME_SEARCH;
        }
      }

      return (int) Duration.between(
              startOfTime,
              time.atZone(startOfTime.getZone())
      ).getSeconds();
    }

    return -1;
  }

  @Override
  public int latestArrivalTime(int requestedArrivalTime) {
    var timeRestrictions = getLastState().getTimeRestrictions();
    if (timeRestrictions.isEmpty()) {
      return requestedArrivalTime;
    }

    var time = startOfTime.plusSeconds(requestedArrivalTime)
            .toLocalDateTime();
    var iterations = 0;

    DATETIME_SEARCH:
    while (iterations < MAX_TIME_RESTRICTION_ITERATIONS) {
      for (final TimeRestrictionWithOffset timeRestriction : timeRestrictions) {
        var offsetFromArrival = timeRestriction.getOffsetInSecondsFromStartOfSearch() - durationInSeconds;
        var timeAtRestriction = time.plusSeconds(offsetFromArrival);
        var traversableAt = timeRestriction.getTimeRestriction()
                .latestArrivalTime(timeAtRestriction);

        if (traversableAt.isEmpty()) {
          break DATETIME_SEARCH;
        }

        var alternateTime = traversableAt.get();
        if (!alternateTime.equals(timeAtRestriction)) {
          time = alternateTime.minusSeconds(offsetFromArrival);
          iterations++;
          continue DATETIME_SEARCH;
        }
      }

      return (int) Duration.between(
              startOfTime,
              time.atZone(startOfTime.getZone())
      ).getSeconds();
    }

    return -1;
  }

  public State getLastState() {
    return lastState;
  }

  @Override
  public String toString() {
    return ToStringBuilder
        .of(AccessEgress.class)
        .addStr("transfer", asString())
        .addObj("state", lastState)
        .toString();
  }
}