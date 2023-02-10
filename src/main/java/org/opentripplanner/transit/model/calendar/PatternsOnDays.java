package org.opentripplanner.transit.model.calendar;

import java.util.Collection;
import java.util.List;

// For all days list the {@link PatternsForDay}
public class PatternsOnDays {

  private final List<PatternsOnDay> days;

  public PatternsOnDays(Collection<PatternsOnDay> patternsOnDay) {
    days = List.copyOf(patternsOnDay);
  }

  public PatternsOnDay patternsOnDay(int day) {
    return days.get(day);
  }
}
