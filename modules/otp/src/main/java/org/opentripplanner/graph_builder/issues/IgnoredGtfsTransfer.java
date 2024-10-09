package org.opentripplanner.graph_builder.issues;

import org.onebusaway.gtfs.model.Transfer;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;

public record IgnoredGtfsTransfer(Transfer transfer) implements DataImportIssue {
  private static final String FMT =
    "GTFS Transfer was ignored, since it doesn't provide any routing advantages: %s";

  @Override
  public String getMessage() {
    return String.format(FMT, transfer);
  }
}
