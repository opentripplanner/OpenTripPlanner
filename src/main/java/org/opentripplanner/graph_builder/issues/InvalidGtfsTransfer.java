package org.opentripplanner.graph_builder.issues;

import org.onebusaway.gtfs.model.Transfer;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;

public class InvalidGtfsTransfer implements DataImportIssue {

  public static final String FMT = "GTFS Transfer was invalid (%s): %s";

  private final String reason;

  private final Transfer transfer;

  public InvalidGtfsTransfer(String reason, Transfer transfer) {
    this.reason = reason;
    this.transfer = transfer;
  }

  @Override
  public String getMessage() {
    return String.format(FMT, reason, transfer);
  }
}
