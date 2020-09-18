package org.opentripplanner.ext.flex.distancecalculator;

import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;

/**
 * Calculated driving times and distance based on direct distance and fixed average driving speed.
 */
public class DirectDistanceCalculator implements DistanceCalculator {
  public static final double FLEX_SPEED = 8.0;

  private static final int DIRECT_EXTRA_TIME = 5 * 60;

  private double flexSpeed;

  public DirectDistanceCalculator(Graph graph) {
    this.flexSpeed = FLEX_SPEED;
  }

  @Override
  public DistanceAndDuration getDuration(
      Vertex fromv, Vertex tov, int fromIndex, int toIndex
  ) {
    double distance = SphericalDistanceLibrary.distance(fromv.getCoordinate(), tov.getCoordinate());

    return new DistanceAndDuration((int) distance, (int) (distance / flexSpeed) + DIRECT_EXTRA_TIME);
  }
}
