package org.opentripplanner.netex.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;

public class QuayWithoutCoordinates implements DataImportIssue {

  public static final String FMT = "Quay %s does not contain any coordinates.";

  final String quayId;

  public QuayWithoutCoordinates(String quayId) {
    this.quayId = quayId;
  }

  @Override
  public String getMessage() {
    return String.format(FMT, quayId);
  }
}
