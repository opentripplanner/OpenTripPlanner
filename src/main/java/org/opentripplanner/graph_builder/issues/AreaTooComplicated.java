package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.openstreetmap.model.OSMWithTags;

public record AreaTooComplicated(OSMWithTags entity, int nbNodes, int maxAreaNodes)
  implements DataImportIssue {
  public static final String FMT = "Area %s is too complicated (%s > %s)";
  public static final String HTMLFMT = "Area <a href='%s'>'%s'</a> is too complicated (%s > %s)";

  public String getMessage() {
    return String.format(FMT, entity.getId(), nbNodes, maxAreaNodes);
  }

  public String getHTMLMessage() {
    return String.format(
      HTMLFMT,
      entity.getOpenStreetMapLink(),
      entity.getId(),
      nbNodes,
      maxAreaNodes
    );
  }

  public int getPriority() {
    return nbNodes;
  }
}
