package org.opentripplanner.netex.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;

public class StopPlaceWithoutCoordinates implements DataImportIssue {

  public static final String FMT = "%s  does not contain any coordinates.";

  final String stopPlaceId;

  public StopPlaceWithoutCoordinates(String stopPlaceId) {
    this.stopPlaceId = stopPlaceId;
  }

  @Override
  public String getMessage() {
    return String.format(FMT, stopPlaceId);
  }
}
