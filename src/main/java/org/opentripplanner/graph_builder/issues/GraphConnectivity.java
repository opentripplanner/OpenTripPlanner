package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.street.search.TraverseMode;

public record GraphConnectivity(
  TraverseMode traverseMode,
  long size,
  long stopIslands,
  long stopIslandsChanged,
  long removed,
  long restricted,
  long nothru
)
  implements DataImportIssue {
  private static final String FMT =
    "%s graph connectivity: found %d islands, %d islands with stops, modified %d islands with stops, removed %d isolated edges, removed traverse mode from %d edges, converted %d edges to no through traffic";

  @Override
  public String getMessage() {
    return String.format(
      FMT,
      this.traverseMode.toString(),
      this.size,
      this.stopIslands,
      this.stopIslandsChanged,
      this.removed,
      this.restricted,
      this.nothru
    );
  }
}
