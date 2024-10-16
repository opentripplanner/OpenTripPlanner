package org.opentripplanner.graph_builder.module.osm;

import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.osm.model.OsmWithTags;

public record AreaTooComplicated(AreaGroup areaGroup, int nbNodes, int maxAreaNodes)
  implements DataImportIssue {
  private static final String FMT = "Area %s is too complicated (%s > %s)";
  private static final String HTMLFMT = "Area <a href='%s'>'%s'</a> is too complicated (%s > %s)";

  @Override
  public String getMessage() {
    return String.format(FMT, areaGroup.getSomeOsmObject().getId(), nbNodes, maxAreaNodes);
  }

  @Override
  public String getHTMLMessage() {
    OsmWithTags entity = areaGroup.getSomeOsmObject();
    return String.format(HTMLFMT, entity.url(), entity.getId(), nbNodes, maxAreaNodes);
  }

  @Override
  public int getPriority() {
    return nbNodes;
  }

  @Override
  public Geometry getGeometry() {
    return areaGroup.union;
  }
}
