package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.openstreetmap.model.OSMWithTags;

public record AreaTooComplicated(OSMWithTags entity, int nbNodes, int maxAreaNodes)
  implements DataImportIssue {
  private static String FMT = "Area %s is too complicated (%s > %s)";
  private static String HTMLFMT = "Area <a href='%s'>'%s'</a> is too complicated (%s > %s)";

  @Override
  public String getMessage() {
    return String.format(FMT, entity.getId(), nbNodes, maxAreaNodes);
  }

  @Override
  public String getHTMLMessage() {
    return String.format(
      HTMLFMT,
      entity.getOpenStreetMapLink(),
      entity.getId(),
      nbNodes,
      maxAreaNodes
    );
  }

  @Override
  public int getPriority() {
    return nbNodes;
  }
}
