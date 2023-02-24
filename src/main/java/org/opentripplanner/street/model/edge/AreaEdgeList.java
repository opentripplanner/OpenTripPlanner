package org.opentripplanner.street.model.edge;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.opentripplanner.street.model.vertex.IntersectionVertex;

/**
 * This is a representation of a set of contiguous OSM areas, used for various tasks related to edge
 * splitting, such as adding new edges during transit linking.
 *
 * @author novalis
 */
public class AreaEdgeList implements Serializable {

  public final HashSet<IntersectionVertex> visibilityVertices = new HashSet<>();

  // these are all of the original edges of the area, whether
  // or not there are corresponding OSM edges. It is used as part of a hack
  // to fix up areas after network linking.
  private final Polygon originalEdges;

  public final Set<String> references;

  private final List<NamedArea> areas = new ArrayList<>();

  public AreaEdgeList(Polygon originalEdges, Set<String> references) {
    this.originalEdges = originalEdges;
    this.references = references;
  }

  public String toString() {
    return String.format(
      "AreaEdgeList: visibilityVertices=%s, %s",
      visibilityVertices,
      originalEdges
    );
  }

  public void addArea(NamedArea namedArea) {
    areas.add(namedArea);
  }

  public List<NamedArea> getAreas() {
    return areas;
  }

  public Geometry getGeometry() {
    return originalEdges;
  }
}
