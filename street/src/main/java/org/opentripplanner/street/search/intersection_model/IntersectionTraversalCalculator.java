package org.opentripplanner.street.search.intersection_model;

import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.search.TraverseMode;

/**
 * An interface to a model that computes the duration of turns.
 * <p>
 * Turn durations are given in seconds - they represent the expected amount of time it would take to
 * make a turn.
 *
 * @author avi
 */

public interface IntersectionTraversalCalculator {
  IntersectionTraversalCalculator DEFAULT = create(
    IntersectionTraversalModel.SIMPLE,
    DrivingDirection.RIGHT
  );
  /**
   * Compute the duration of turning onto "to" from "from".
   *
   * @return expected number of seconds the traversal is expected to take.
   */
  double computeTraversalDuration(
    IntersectionVertex v,
    StreetEdge from,
    StreetEdge to,
    TraverseMode mode,
    float fromSpeed,
    float toSpeed
  );

  static IntersectionTraversalCalculator create(
    IntersectionTraversalModel intersectionTraversalModel,
    DrivingDirection drivingDirection
  ) {
    return switch (intersectionTraversalModel) {
      case SIMPLE -> new SimpleIntersectionTraversalCalculator(drivingDirection);
      case CONSTANT -> new ConstantIntersectionTraversalCalculator();
    };
  }
}
