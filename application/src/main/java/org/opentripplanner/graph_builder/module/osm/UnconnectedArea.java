package org.opentripplanner.graph_builder.module.osm;

import java.util.stream.Collectors;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;

public record UnconnectedArea(OsmAreaGroup areaGroup) implements DataImportIssue {
  private static final String FMT = "Area %s has no connection to street network";
  private static final String HTMLFMT =
    "Area <a href='%s'>'%s'</a> has no connection to street network";

  @Override
  public String getMessage() {
    return String.format(FMT, idList());
  }

  @Override
  public String getHTMLMessage() {
    return String.format(HTMLFMT, areaGroup.getSomeOsmObject().url(), idList());
  }

  @Override
  public Geometry getGeometry() {
    return areaGroup.union;
  }

  private String idList() {
    return areaGroup.areas
      .stream()
      .map(area -> area.parent.getId())
      .map(Object::toString)
      .collect(Collectors.joining(", "));
  }
}
