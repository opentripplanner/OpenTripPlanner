package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.model.StopTime;

public class NegativeHopTime implements DataImportIssue {

  public static final String FMT =
    "Negative time hop between %s and %s; skipping the entire trip. " +
    "This might be caused by the use of 00:xx instead of 24:xx for stoptimes after midnight.";

  public final StopTime st0, st1;

  public NegativeHopTime(StopTime st0, StopTime st1) {
    this.st0 = st0;
    this.st1 = st1;
  }

  @Override
  public String getMessage() {
    return String.format(FMT, st0, st1);
  }
}
