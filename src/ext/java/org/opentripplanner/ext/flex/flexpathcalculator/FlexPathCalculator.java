package org.opentripplanner.ext.flex.flexpathcalculator;

import org.opentripplanner.model.StopLocation;
import org.opentripplanner.routing.graph.Vertex;

import javax.annotation.Nullable;

/**
 * FlexPathCalculator is used to calculate the driving times and distances during flex routing
 */
public interface FlexPathCalculator {

  @Nullable
  FlexPath calculateFlexPath(Vertex fromv, Vertex tov, StopLocation s1, StopLocation s2, int fromStopIndex, int toStopIndex);

}
