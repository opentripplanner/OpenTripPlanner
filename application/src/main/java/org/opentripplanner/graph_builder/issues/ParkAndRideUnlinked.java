package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.osm.model.OsmEntity;

public record ParkAndRideUnlinked(String name, OsmEntity entity) implements DataImportIssue {
  private static final String FMT =
    "Park and ride '%s' (%s) not linked to any streets in OSM; entrance might not be placed correctly.";
  private static final String HTMLFMT =
    "Park and ride <a href='%s'>'%s' (%s)</a> not linked to any streets in OSM; entrance might not be placed correctly.";

  @Override
  public String getMessage() {
    return String.format(FMT, name, entity);
  }

  @Override
  public String getHTMLMessage() {
    return String.format(HTMLFMT, entity.url(), name, entity);
  }
}
