package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;

public class MissingProjectionInServiceLink implements DataImportIssue {

  public static final String FMT =
    "Creating straight line path between Quays for ServiceLink with missing projection: %s";

  final String serviceLinkId;

  public MissingProjectionInServiceLink(String serviceLinkId) {
    this.serviceLinkId = serviceLinkId;
  }

  @Override
  public String getMessage() {
    return String.format(FMT, serviceLinkId);
  }
}
