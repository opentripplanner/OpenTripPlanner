package org.opentripplanner.street.model.edge;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.opentripplanner.street.model.vertex.IntersectionVertex;

/**
 * This is a representation of a set of contiguous OSM areas, used for various tasks related to edge
 * splitting, such as adding new edges during transit linking.
 */
public class AreaGroup implements Serializable {

  // Mutable backing set: callers see an unmodifiable view; addVisibilityVertex may append at runtime.
  private final HashSet<IntersectionVertex> visibilityVertices;
  private final Polygon geometry;
  private final List<Area> areas;

  private AreaGroup(Builder builder) {
    this.geometry = builder.geometry;
    this.areas = List.copyOf(builder.areas);
    this.visibilityVertices = new HashSet<>(builder.visibilityVertices);
  }

  public static Builder of(Polygon geometry) {
    return new Builder(geometry);
  }

  public String toString() {
    return String.format("AreaGroup: visibilityVertices=%s, %s", visibilityVertices, geometry);
  }

  public List<Area> getAreas() {
    return areas;
  }

  public Geometry getGeometry() {
    return geometry;
  }

  /**
   * Returns the set of visibility vertices.
   */
  public Set<IntersectionVertex> visibilityVertices() {
    return Collections.unmodifiableSet(visibilityVertices);
  }

  /**
   * Append a vertex discovered during permanent transit-stop linking. This is the only
   * post-construction mutation allowed on AreaGroup; it ensures future linking operations
   * can reach this vertex without re-linking the entire area.
   */
  public void addVisibilityVertex(IntersectionVertex vertex) {
    visibilityVertices.add(vertex);
  }

  public static class Builder {

    private final Polygon geometry;
    private final List<Area> areas = new ArrayList<>();
    private Set<IntersectionVertex> visibilityVertices = Set.of();

    private Builder(Polygon geometry) {
      this.geometry = geometry;
    }

    public Builder addArea(Area area) {
      areas.add(area);
      return this;
    }

    public Builder withVisibilityVertices(Set<IntersectionVertex> vertices) {
      this.visibilityVertices = vertices;
      return this;
    }

    public AreaGroup build() {
      return new AreaGroup(this);
    }
  }
}
