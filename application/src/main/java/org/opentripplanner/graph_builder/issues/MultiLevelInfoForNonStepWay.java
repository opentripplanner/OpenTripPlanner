package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.osm.model.OsmEntity;

public record MultiLevelInfoForNonStepWay(String layerName, OsmEntity entity)
  implements DataImportIssue {
  private static final String FMT =
    "Way contained multi-level info '%s' for non-step way (no 'highway=steps' tag): %s";
  private static final String HTMLFMT =
    "Way contained multi-level info '%s' for non-step way (no 'highway=steps' tag): <a href='%s'>'%s'</a>";

  @Override
  public String getMessage() {
    return String.format(FMT, layerName, entity.getId());
  }

  @Override
  public String getHTMLMessage() {
    return String.format(HTMLFMT, layerName, entity.url(), entity.getId());
  }
}
