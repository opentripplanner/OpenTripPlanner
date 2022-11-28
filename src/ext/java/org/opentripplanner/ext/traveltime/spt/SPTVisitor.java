package org.opentripplanner.ext.traveltime.spt;

import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.search.state.State;

public interface SPTVisitor {
  /**
   * @param e The edge to filter.
   * @return True to visit this edge, false to skip it.
   */
  boolean accept(Edge e);

  /**
   * Note: The same state can be visited several times (from different edges).
   *
   * @param e     The edge being visited (filtered from a previous call to accept)
   * @param c     The coordinate of the point alongside the edge geometry.
   * @param s0    The state at the start vertex of this edge
   * @param s1    The state at the end vertex of this edge
   * @param d0    Curvilinear coordinate of c on [s0-s1], in meters
   * @param d1    Curvilinear coordinate of c on [s1-s0], in meters
   * @param speed The assumed speed on the edge
   */
  void visit(Edge e, Coordinate c, State s0, State s1, double d0, double d1, double speed);
}
