package org.opentripplanner.osm.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.osm.model.OsmEntity;

public record MoreThanTwoLevelsForWay(String level, OsmEntity entity) implements DataImportIssue {
  private static final String FMT = "%s : way contained more than two levels: %s.";

  private static final String HTMLFMT =
    "<a href='%s'>'%s'</a> : way contained more than two levels: %s.";

  @Override
  public String getMessage() {
    return String.format(FMT, entity.getId(), level);
  }

  @Override
  public String getHTMLMessage() {
    return String.format(HTMLFMT, entity.url(), entity.getId(), level);
  }
}
