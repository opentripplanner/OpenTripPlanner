package org.opentripplanner.netex.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;

public class DayTypeScheduleIsEmpty implements DataImportIssue {

  private final String dayTypeId;

  public DayTypeScheduleIsEmpty(String dayTypeId) {
    this.dayTypeId = dayTypeId;
  }

  @Override
  public String getMessage() {
    return String.format(
      "DayType calendar (set of operating days) is empty. DayType=%s.",
      dayTypeId
    );
  }
}
