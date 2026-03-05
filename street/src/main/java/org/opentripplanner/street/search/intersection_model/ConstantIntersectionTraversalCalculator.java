package org.opentripplanner.street.search.intersection_model;

import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.search.TraverseMode;

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
