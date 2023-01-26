package org.opentripplanner.graph_builder.issues;

import org.onebusaway.gtfs.model.Transfer;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;

public class IgnoredGtfsTransfer implements DataImportIssue {

  public static final String FMT =
    "GTFS Transfer was ignored, since it doesn't provide any routing advantages: %s";

  private final Transfer transfer;

  public IgnoredGtfsTransfer(Transfer transfer) {
    this.transfer = transfer;
  }

  @Override
  public String getMessage() {
    return String.format(FMT, transfer);
  }
}
