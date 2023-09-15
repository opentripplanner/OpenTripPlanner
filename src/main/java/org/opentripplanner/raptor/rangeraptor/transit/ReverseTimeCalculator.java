package org.opentripplanner.raptor.rangeraptor.transit;

import org.opentripplanner.raptor.api.model.RaptorConstants;

/**
 * A calculator that will take you back in time not forward, this is the basic logic to implement a
 * reverse search.
 * <p>
 * This class is stateless, but need to be instantiatable to be injected as a strategy.
 */
public class ReverseTimeCalculator implements TimeCalculator {

  @Override
  public final boolean searchForward() {
    return false;
  }

  @Override
  public final int plusDuration(final int time, final int duration) {
    // It might seem strange to use minus int the add method, but
    // the "positive" direction in this class is backwards in time;
    // hence we need to subtract the board slack.
    return time - duration;
  }

  @Override
  public final int minusDuration(final int time, final int duration) {
    // It might seem strange to use plus int the subtract method, but
    // the "positive" direction in this class is backwards in time;
    // hence we need to add the board slack.
    return time + duration;
  }

  @Override
  public final int duration(final int timeA, final int timeB) {
    // When searching in reverse time A is > time B, so to
    // calculate the duration we need to swap A and B
    // compared with the normal forward search
    return timeA - timeB;
  }

  @Override
  public final boolean isBefore(final int subject, final int candidate) {
    return subject > candidate;
  }

  @Override
  public final boolean isAfter(int subject, int candidate) {
    return subject < candidate;
  }

  @Override
  public final int unreachedTime() {
    return RaptorConstants.TIME_UNREACHED_REVERSE;
  }
}
