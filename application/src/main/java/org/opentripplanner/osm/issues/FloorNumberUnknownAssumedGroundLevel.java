package org.opentripplanner.osm.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.osm.model.OsmEntity;

public record FloorNumberUnknownAssumedGroundLevel(String level, OsmEntity entity)
  implements DataImportIssue {
  private static final String FMT =
    "%s : could not determine level from 'level' or 'layer' tag '%s', assumed to be ground-level.";

  private static final String HTMLFMT =
    "<a href='%s'>'%s'</a> : could not determine level from 'level' or 'layer' tag '%s', " +
    "assumed to be ground-level.";

  @Override
  public String getMessage() {
    return String.format(FMT, entity.getId(), level);
  }

  @Override
  public String getHTMLMessage() {
    return String.format(HTMLFMT, entity.url(), entity.getId(), level);
  }
}
