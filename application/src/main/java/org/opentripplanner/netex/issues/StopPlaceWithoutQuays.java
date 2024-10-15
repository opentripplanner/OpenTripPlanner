package org.opentripplanner.netex.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;

public class StopPlaceWithoutQuays implements DataImportIssue {

  public static final String FMT = "%s  does not contain any quays.";

  final String stopPlaceId;

  public StopPlaceWithoutQuays(String stopPlaceId) {
    this.stopPlaceId = stopPlaceId;
  }

  @Override
  public String getMessage() {
    return String.format(FMT, stopPlaceId);
  }
}
