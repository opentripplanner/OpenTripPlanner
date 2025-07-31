package org.opentripplanner.street.model.vertex;

import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.StreetEdge;

/**
 * This vertex is created from all barrier tags.
 * <p>
 * On this vertex geometry is split and two new edges are created.
 * <p>
 * If start/end vertex of {@link StreetEdge} is BarrierVertex
 * edge isn't traversable with CAR.
 */
public class BarrierVertex extends OsmVertex {

  public static final StreetTraversalPermission defaultBarrierPermissions =
    StreetTraversalPermission.ALL;
  private StreetTraversalPermission barrierPermissions;

  public BarrierVertex(double x, double y, long nodeId) {
    super(x, y, nodeId);
    barrierPermissions = defaultBarrierPermissions;
  }

  public StreetTraversalPermission getBarrierPermissions() {
    return barrierPermissions;
  }

  public void setBarrierPermissions(StreetTraversalPermission barrierPermissions) {
    this.barrierPermissions = barrierPermissions;
  }

  /*
   * Barrier vertex at the end of a way does not make sense, because
   * it creates discontinuity of routing in a single point.
   * This method examines if traversal limitations can be removed.
   * The logic examines edges referring to the vertex, so it should be
   * applied only after the vertex has been linked to the graph.
   */
  public void makeBarrierAtEndReachable() {
    var edgeCount = this.getDegreeOut() + this.getDegreeIn();
    var needsFix = false;
    if (edgeCount == 1) {
      // only one edge connects the vertex, must be end point
      needsFix = true;
    } else if (edgeCount == 2) {
      var out = this.getOutgoing();
      var in = this.getIncoming();
      if (
        // if only outgoing edges or incoming edges -> vertex does not act as a pass-through point and barrier makes no sense
        out.isEmpty() ||
        in.isEmpty() ||
        // in+out edge pair connects the vertex to a single adjacent vertex -> must be street end point
        out.iterator().next().getToVertex() ==
        in.iterator().next().getFromVertex()
      ) {
        needsFix = true;
      }
    }
    if (needsFix) {
      this.barrierPermissions = StreetTraversalPermission.ALL;
    }
  }
}
