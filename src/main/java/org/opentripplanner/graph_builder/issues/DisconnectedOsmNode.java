package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.openstreetmap.model.OSMWithTags;

public record DisconnectedOsmNode(OSMWithTags node, OSMWithTags way, OSMWithTags area)
  implements DataImportIssue {
  private static String FMT = "Node %s in way %s is coincident but disconnected with area %s";
  private static String HTMLFMT =
    "Node<a href='%s'>'%s'</a> in way <a href='%s'>'%s'</a> is coincident but disconnected with area <a href='%s'>'%s'</a>";

  @Override
  public String getMessage() {
    return String.format(FMT, node.getId(), way.getId(), area.getId());
  }

  @Override
  public String getHTMLMessage() {
    return String.format(
      HTMLFMT,
      node.getOpenStreetMapLink(),
      node.getId(),
      way.getOpenStreetMapLink(),
      way.getId(),
      area.getOpenStreetMapLink(),
      area.getId()
    );
  }
}
