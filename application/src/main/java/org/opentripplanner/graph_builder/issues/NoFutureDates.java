package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;

public record NoFutureDates(String agency) implements DataImportIssue {
  private static final String FMT =
    "Agency %s has no calendar dates which are after today; " +
    "no trips will be plannable on this agency";

  @Override
  public String getMessage() {
    return String.format(FMT, agency);
  }
}
