package org.opentripplanner.street.model.edge;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

  private static final Set<IntersectionVertex> EMPTY_SET = Set.of();
  private Set<IntersectionVertex> visibilityVertices = EMPTY_SET;
  private final Polygon geometry;
  private final List<NamedArea> areas = new ArrayList<>();

  public AreaEdgeList(Polygon geometry) {
    this.geometry = geometry;
  }

  public String toString() {
    return String.format("AreaEdgeList: visibilityVertices=%s, %s", visibilityVertices, geometry);
  }

  public void addArea(NamedArea namedArea) {
    areas.add(namedArea);
  }

  public List<NamedArea> getAreas() {
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
   * Add a visibility vertex to this edge.
   */
  public void addVisibilityVertex(IntersectionVertex toBeAdded) {
    Objects.requireNonNull(toBeAdded);
    synchronized (this) {
      if (visibilityVertices == EMPTY_SET) {
        visibilityVertices = Set.of(toBeAdded);
      } else {
        visibilityVertices =
          Stream
            .concat(visibilityVertices.stream(), Stream.of(toBeAdded))
            .collect(Collectors.toUnmodifiableSet());
      }
    }
  }
}
