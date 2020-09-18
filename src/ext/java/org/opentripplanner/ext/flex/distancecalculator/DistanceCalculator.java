package org.opentripplanner.ext.flex.distancecalculator;

import org.opentripplanner.routing.graph.Vertex;

/**
 * DistanceCalculator is used to calculate the driving times and distances during flex routing
 */
public interface DistanceCalculator {
  DistanceAndDuration getDuration(Vertex fromv, Vertex tov, int fromIndex, int toIndex);

}
