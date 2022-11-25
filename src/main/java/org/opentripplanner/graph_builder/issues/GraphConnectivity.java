package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.street.search.TraverseMode;

public class GraphConnectivity implements DataImportIssue {

  public static final String FMT =
    "%s graph connectivity: found %d islands, %d islands with stops, modified %d islands with stops, removed %d isolated edges, removed traverse mode from %d edges, converted %d edges to no through traffic";

  final TraverseMode traverseMode;
  final long size;
  final long stopIslands;
  final long stopIslandsChanged;
  final long removed;
  final long restricted;
  final long nothru;

  public GraphConnectivity(
    TraverseMode traverseMode,
    long size,
    long stopIslands,
    long stopIslandsChanged,
    long removed,
    long restricted,
    long nothru
  ) {
    this.traverseMode = traverseMode;
    this.size = size;
    this.stopIslands = stopIslands;
    this.stopIslandsChanged = stopIslandsChanged;
    this.removed = removed;
    this.restricted = restricted;
    this.nothru = nothru;
  }

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

  @Override
  public String getHTMLMessage() {
    return this.getMessage();
  }
}
