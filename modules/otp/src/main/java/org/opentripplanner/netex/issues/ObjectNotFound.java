package org.opentripplanner.netex.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;

public class ObjectNotFound implements DataImportIssue {

  private final String sourceType;
  private final String sourceId;
  private final String targetFieldName;
  private final String missingTargetId;

  public ObjectNotFound(
    String sourceType,
    String sourceId,
    String targetFieldName,
    String missingTargetId
  ) {
    this.sourceType = sourceType;
    this.sourceId = sourceId;
    this.targetFieldName = targetFieldName;
    this.missingTargetId = missingTargetId;
  }

  @Override
  public String getMessage() {
    return String.format(
      "Object not found. %s %s is not found in import. Id = %s, %s = %s",
      sourceType,
      targetFieldName,
      sourceId,
      targetFieldName,
      missingTargetId
    );
  }
}
