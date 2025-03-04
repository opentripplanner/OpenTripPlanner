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
public class AreaGroup implements Serializable {

  private static final Set<IntersectionVertex> EMPTY_SET = Set.of();
  private Set<IntersectionVertex> visibilityVertices = EMPTY_SET;
  private final Polygon geometry;
  private final List<Area> areas = new ArrayList<>();

  public AreaGroup(Polygon geometry) {
    this.geometry = geometry;
  }

  public String toString() {
    return String.format("AreaGroup: visibilityVertices=%s, %s", visibilityVertices, geometry);
  }

  public void addArea(Area area) {
    areas.add(area);
  }

  public List<Area> getAreas() {
    return areas;
  }

  public Geometry getGeometry() {
    return geometry;
  }

  /**
   * Returns the list of visibility vertices.
   */
  public Set<IntersectionVertex> visibilityVertices() {
    return visibilityVertices;
  }

  /**
   * Add a set of visibility vertices to this area group
   */
  public void addVisibilityVertices(Set<IntersectionVertex> toAdd) {
    synchronized (this) {
      if (visibilityVertices == EMPTY_SET) {
        visibilityVertices = Set.copyOf(toAdd);
      } else {
        var temp = new HashSet<>(visibilityVertices);
        temp.addAll(toAdd);
        visibilityVertices = Set.copyOf(temp);
      }
    }
  }
}
