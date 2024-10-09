package org.opentripplanner.raptor.rangeraptor.transit;

import org.opentripplanner.raptor.api.model.RaptorConstants;

/**
 * Used to calculate times in a forward trip search.
 * <p>
 * This class is stateless, but need to be instantiatable to be injected as a strategy.
 */
public class ForwardTimeCalculator implements TimeCalculator {

  @Override
  public final boolean searchForward() {
    return true;
  }

  @Override
  public final int plusDuration(final int time, final int delta) {
    return time + delta;
  }

  @Override
  public final int minusDuration(final int time, final int delta) {
    return time - delta;
  }

  @Override
  public final int duration(final int timeA, final int timeB) {
    return timeB - timeA;
  }

  @Override
  public final boolean isBefore(final int subject, final int candidate) {
    return subject < candidate;
  }

  @Override
  public final boolean isAfter(int subject, int candidate) {
    return subject > candidate;
  }

  @Override
  public final int unreachedTime() {
    return RaptorConstants.TIME_UNREACHED_FORWARD;
  }
}
