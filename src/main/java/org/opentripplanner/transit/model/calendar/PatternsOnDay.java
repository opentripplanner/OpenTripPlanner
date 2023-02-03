package org.opentripplanner.transit.model.calendar;

// TODO RTM

import java.util.Collection;
import java.util.List;

/**
 * For a given day (starter at 04:00 until 04:00 D+1), list all patterns relevant for boarding or
 * alighting that day.
 */
public record PatternsOnDay(Collection<PatternOnDay> patterns) {
  public PatternsOnDay(Collection<PatternOnDay> patterns) {
    this.patterns = List.copyOf(patterns);
  }
}
