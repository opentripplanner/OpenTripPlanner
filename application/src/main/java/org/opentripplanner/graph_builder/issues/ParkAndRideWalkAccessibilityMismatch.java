package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.osm.model.OsmEntity;

public record ParkAndRideWalkAccessibilityMismatch(OsmEntity entity) implements DataImportIssue {
  private static final String FMT =
    "Park and ride '%d' (%s) has inconsistent walk access: it is accessible on foot in one direction but not the other.";
  private static final String HTMLFMT =
    "Park and ride <a href='%s'>'%d'</a> has inconsistent walk access: it is accessible on foot in one direction but not the other.";

  @Override
  public String getMessage() {
    return String.format(FMT, entity.getId(), entity);
  }

  @Override
  public String getHTMLMessage() {
    return String.format(HTMLFMT, entity.url(), entity.getId());
  }
}
