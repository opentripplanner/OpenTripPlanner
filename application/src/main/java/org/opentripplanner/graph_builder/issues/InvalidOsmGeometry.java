package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.osm.model.OsmEntity;

public record InvalidOsmGeometry(OsmEntity entity) implements DataImportIssue {
  private static final String FMT = "Invalid OSM geometry %s";
  private static final String HTMLFMT = "Invalid OSM geometry <a href='%s'>'%s'</a>";

  @Override
  public String getMessage() {
    return String.format(FMT, entity.getId());
  }

  @Override
  public String getHTMLMessage() {
    return String.format(HTMLFMT, entity.url(), entity.getId());
  }
}
