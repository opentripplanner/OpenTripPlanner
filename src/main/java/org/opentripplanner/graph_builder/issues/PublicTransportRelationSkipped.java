package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;

public class PublicTransportRelationSkipped implements DataImportIssue {

  public static final String FMT = "Unable to process public transportation relation %s";

  final long relationId;

  public PublicTransportRelationSkipped(long relationId) {
    this.relationId = relationId;
  }

  @Override
  public String getMessage() {
    return String.format(FMT, relationId);
  }
}
