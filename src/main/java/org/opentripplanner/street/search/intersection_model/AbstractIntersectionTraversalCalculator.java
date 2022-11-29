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
   * TODO(flamholz): this should probably account for whether there is a traffic light?
   */
  protected double computeNonDrivingTraversalDuration(
    StreetEdge from,
    StreetEdge to,
    float toSpeed
  ) {
    int outAngle = to.getOutAngle();
    int inAngle = from.getInAngle();
    int turnCost = Math.abs(outAngle - inAngle);
    if (turnCost > 180) {
      turnCost = 360 - turnCost;
    }

    // NOTE: This makes the turn duration lower the faster you're going
    return (this.nonDrivingTurnDurationFactor * turnCost) / toSpeed;
  }

  /**
   * Calculates the turn angle from the incoming/outgoing edges and routing request.
   * <p>
   * Corrects for the side of the street they are driving on.
   */
  protected int calculateTurnAngle(StreetEdge from, StreetEdge to) {
    int angleOutOfIntersection = to.getInAngle();
    int angleIntoIntersection = from.getOutAngle();

    // Put out to the right of in; i.e. represent everything as one long right turn
    // Also ensures that turnAngle is always positive.
    if (angleOutOfIntersection < angleIntoIntersection) {
      angleOutOfIntersection += 360;
    }

    return angleOutOfIntersection - angleIntoIntersection;
  }
}
