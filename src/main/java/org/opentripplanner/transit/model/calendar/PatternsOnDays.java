package org.opentripplanner.transit.model.calendar;

import java.util.List;

// For all days list the {@link PatternsForDay}
public class PatternsOnDays {

  private List<PatternsOnDay> days = List.of();

  public PatternsOnDay patternsOnDay(int day) {
    return days.get(day);
  }
}
