package org.opentripplanner.netex.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;

public class InvalidTimeZone implements DataImportIssue {
  private final String stopPlaceId;
  private final String value;

  public InvalidTimeZone(String stopPlaceId, String value) {
    this.stopPlaceId = stopPlaceId;
    this.value = value;
  }

  @Override
  public String getMessage() {
    return String.format(
      "Invalid ID for ZoneOffset at StopPlace with ID: %s and value %s",
      stopPlaceId, value
    );
  }
}
