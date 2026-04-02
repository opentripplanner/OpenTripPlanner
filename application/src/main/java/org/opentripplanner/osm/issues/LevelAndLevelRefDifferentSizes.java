package org.opentripplanner.osm.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.osm.model.OsmEntity;

public record LevelAndLevelRefDifferentSizes(
  int levelListLength,
  int nameListLength,
  OsmEntity entity
) implements DataImportIssue {
  private static final String FMT =
    "%s : 'level:ref' info can not be used because " +
    "the 'level' tag and 'level:ref' tag refer to a different amount of levels. " +
    "The 'level' tag contains %s levels while 'level:ref' contains %s.";

  private static final String HTMLFMT =
    "<a href='%s'>'%s'</a> : 'level:ref' info can not be used because " +
    "the 'level' tag and 'level:ref' tag refer to a different amount of levels. " +
    "The 'level' tag contains %s levels while 'level:ref' contains %s.";

  @Override
  public String getMessage() {
    return String.format(FMT, entity.getId(), levelListLength, nameListLength);
  }

  @Override
  public String getHTMLMessage() {
    return String.format(HTMLFMT, entity.url(), entity.getId(), levelListLength, nameListLength);
  }
}
