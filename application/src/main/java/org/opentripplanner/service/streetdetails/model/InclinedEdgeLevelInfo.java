package org.opentripplanner.service.streetdetails.model;

import java.io.Serializable;
import java.util.Objects;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.OsmVertex;

/**
 * Represents level information for an edge. The information is represented as two
 * {@link VertexLevelInfo} objects for the first and last vertices of an edge. The lower vertex
 * is represented by lowerVertexInfo and the higher one by upperVertexInfo.
 */
public class InclinedEdgeLevelInfo implements Serializable {

  private final VertexLevelInfo lowerVertexInfo;
  private final VertexLevelInfo upperVertexInfo;

  public InclinedEdgeLevelInfo(VertexLevelInfo lowerVertexInfo, VertexLevelInfo upperVertexInfo) {
    this.lowerVertexInfo = Objects.requireNonNull(lowerVertexInfo);
    this.upperVertexInfo = Objects.requireNonNull(upperVertexInfo);
  }

  /**
   * Checks if the vertices of the edge match the nodeIds found in the {@link VertexLevelInfo}
   * objects. In other words this function checks if the edge level information in this object
   * can be used for the given edge.
   */
  public boolean canBeAppliedToEdge(Edge edge) {
    return (
      edge.getToVertex() instanceof OsmVertex toVertex &&
      edge.getFromVertex() instanceof OsmVertex fromVertex &&
      ((lowerVertexInfo.osmNodeId() == fromVertex.nodeId() &&
          upperVertexInfo.osmNodeId() == toVertex.nodeId()) ||
        (lowerVertexInfo.osmNodeId() == toVertex.nodeId() &&
          upperVertexInfo.osmNodeId() == fromVertex.nodeId()))
    );
  }

  public VertexLevelInfo lowerVertexInfo() {
    return lowerVertexInfo;
  }

  public VertexLevelInfo upperVertexInfo() {
    return upperVertexInfo;
  }

  @Override
  public int hashCode() {
    return Objects.hash(lowerVertexInfo, upperVertexInfo);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o == null || o.getClass() != getClass()) {
      return false;
    }
    InclinedEdgeLevelInfo that = (InclinedEdgeLevelInfo) o;
    return (
      Objects.equals(lowerVertexInfo, that.lowerVertexInfo) &&
      Objects.equals(upperVertexInfo, that.upperVertexInfo)
    );
  }
}
