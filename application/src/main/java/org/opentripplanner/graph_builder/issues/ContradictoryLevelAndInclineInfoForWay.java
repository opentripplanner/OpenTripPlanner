package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.osm.model.OsmWay;

public record ContradictoryLevelAndInclineInfoForWay(OsmWay way) implements DataImportIssue {
  private static final String FMT =
    "Way %s has contradictory level information in the 'incline' and 'level'/'layer' tags. " +
    "Please verify that the tags indicate the same vertical direction.";

  private static final String HTMLFMT =
    "<a href='%s'>Way %s</a> has contradictory level information in the 'incline' and 'level'/'layer' tags. " +
    "Please verify that the tags indicate the same vertical direction.";

  @Override
  public String getMessage() {
    return String.format(FMT, way.getId());
  }

  @Override
  public String getHTMLMessage() {
    return String.format(HTMLFMT, way.url(), way.getId());
  }
}
