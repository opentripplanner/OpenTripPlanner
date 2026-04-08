package org.opentripplanner.graph_builder.issues;

import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.street.geometry.GeometryUtils;

public record ContradictoryLevelAndInclineInfoForWay(
  OsmWay way,
  Coordinate from,
  Coordinate to
) implements DataImportIssue {
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

  @Override
  public Geometry getGeometry() {
    return GeometryUtils.makeLineString(List.of(from, to));
  }
}
