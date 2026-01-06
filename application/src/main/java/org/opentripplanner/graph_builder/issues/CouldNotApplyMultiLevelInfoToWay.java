package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.osm.model.OsmWay;

public record CouldNotApplyMultiLevelInfoToWay(OsmWay way, int nodes) implements DataImportIssue {
  private static final String FMT =
    "Multi-level info for way %s can not be used because node references did not match. " +
    "This is probably caused by more than 2 intersection nodes in the way. " +
    "The way has %s nodes in total.";

  private static final String HTMLFMT =
    "Multi-level info for <a href='%s'>way %s</a> can not be used because node references did not match. " +
    "This is probably caused by more than 2 intersection nodes in the way. " +
    "The way has %s nodes in total.";

  @Override
  public String getMessage() {
    return String.format(FMT, way.getId(), nodes);
  }

  @Override
  public String getHTMLMessage() {
    return String.format(HTMLFMT, way.url(), way.getId(), nodes);
  }
}
