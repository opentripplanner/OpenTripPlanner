package org.opentripplanner.routing.algorithm.transferoptimization.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;
import org.opentripplanner.routing.algorithm.transferoptimization.api.OptimizedPath;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;


/**
 * Utility class with a functon to filter a list of paths based on a cost function
 */
public class FilterPathsUtil {


  /**
   * Filter paths based on given {@code costFunction}. Keep all paths witch have a
   * cost equal to the minimum cost across all paths in the given input {@code paths}.
   */
  public static <T extends RaptorTripSchedule> List<OptimizedPath<T>> filter(
      List<OptimizedPath<T>> paths,
      ToIntFunction<OptimizedPath<T>> costFunction
  ) {
    if(costFunction == null || paths.isEmpty()) {
      return paths;
    }

    List<OptimizedPath<T>> result = new ArrayList<>();
    int minCost = Integer.MAX_VALUE;

    for (OptimizedPath<T> it : paths) {
      int cost = costFunction.applyAsInt(it);
      if(cost > minCost) { continue; }

      if(cost < minCost) {
        minCost = cost;
        result.clear();
      }
      result.add(it);
    }
    return result;
  }
}
