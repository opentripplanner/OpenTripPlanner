package org.opentripplanner.street.search.intersection_model;

import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.search.TraverseMode;

/**
 * Abstract turn calculator model provides various methods most implementations will use.
 *
 * @author avi
 */
public abstract class AbstractIntersectionTraversalCalculator
  implements IntersectionTraversalCalculator {

  /** Factor by which absolute turn angles are divided to get turn durations for non-driving scenarios. */
  protected double nonDrivingTurnDurationFactor = 1.0 / 20.0;

  /* Concrete subclasses must implement this */
  @Override
  public abstract double computeTraversalDuration(
    IntersectionVertex v,
    StreetEdge from,
    StreetEdge to,
    TraverseMode mode,
    float fromSpeed,
    float toSpeed
  );

  /**
   * Computes the turn duration in seconds for non-driving traversal modes.
   * <p>
   */
  protected double computeNonDrivingTraversalDuration(
    StreetEdge from,
    StreetEdge to,
    float toSpeed
  ) {
    int turnCost = Math.abs(calculateTurnAngle(from, to));

    // NOTE: This makes the turn duration lower the faster you're going
    return (this.nonDrivingTurnDurationFactor * turnCost) / toSpeed;
  }

  /**
   * Calculates the turn angle from the incoming/outgoing edges and routing request.
   * <p>
   * The turn angle is specified as negative for left turn, positive for right turn, between
   * -180 and 180.
   */
  public static int calculateTurnAngle(StreetEdge from, StreetEdge to) {
    int angleOutOfIntersection = to.getInAngle();
    int angleIntoIntersection = from.getOutAngle();

    var turnAngle = angleOutOfIntersection - angleIntoIntersection;

    // ensure that the turn angle is between -180 and 180
    if (turnAngle >= 180) {
      turnAngle -= 360;
    }
    if (turnAngle < -180) {
      turnAngle += 360;
    }
    return turnAngle;
  }
}
