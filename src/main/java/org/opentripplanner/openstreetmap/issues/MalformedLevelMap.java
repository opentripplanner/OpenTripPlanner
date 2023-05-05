package org.opentripplanner.openstreetmap.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.openstreetmap.model.OSMRelation;

public class MalformedLevelMap implements DataImportIssue {

  public static final String FMT =
    "Could not parse level map for relation id %s as it was malformed, skipped.";

  final long relationId;

  public MalformedLevelMap(OSMRelation relation) {
    this.relationId = relation.getId();
  }

  @Override
  public String getMessage() {
    return String.format(FMT, relationId);
  }
}
