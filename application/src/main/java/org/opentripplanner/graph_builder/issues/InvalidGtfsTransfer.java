package org.opentripplanner.graph_builder.issues;

import org.onebusaway.gtfs.model.Transfer;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;

public record InvalidGtfsTransfer(String reason, Transfer transfer) implements DataImportIssue {
  private static final String FMT = "GTFS Transfer was invalid (%s): %s";

  @Override
  public String getMessage() {
    return String.format(FMT, reason, transfer);
  }
}
