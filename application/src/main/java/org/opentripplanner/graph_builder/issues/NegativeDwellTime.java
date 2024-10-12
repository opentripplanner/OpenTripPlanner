package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.model.StopTime;

public record NegativeDwellTime(StopTime stop) implements DataImportIssue {
  private static final String FMT = "Negative time dwell at %s; skipping the entire trip.";

  @Override
  public String getMessage() {
    return String.format(FMT, stop);
  }
}
