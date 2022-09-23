package org.opentripplanner.routing.core.intersection_model;

import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.vertextype.IntersectionVertex;

/**
 * The duration of traversing an intersection is constant.
 */
public class ConstantIntersectionTraversalCalculator
  extends AbstractIntersectionTraversalCalculator {

  private final double duration;

  /**
   * All traversal costs are equal to the passed-in constant.
   */
  public ConstantIntersectionTraversalCalculator(double duration) {
    this.duration = duration;
  }

  /**
   * Convenience constructor for no cost.
   */
  public ConstantIntersectionTraversalCalculator() {
    this(0.0);
  }

  @Override
  public double computeTraversalDuration(
    IntersectionVertex v,
    StreetEdge from,
    StreetEdge to,
    TraverseMode mode,
    float fromSpeed,
    float toSpeed
  ) {
    return duration;
  }
}
