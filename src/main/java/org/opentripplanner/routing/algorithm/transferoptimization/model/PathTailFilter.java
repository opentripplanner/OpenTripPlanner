package org.opentripplanner.routing.algorithm.transferoptimization.model;

import java.util.Set;

/**
 * PathTailFilter is used to filter out suboptimal paths during transfer optimization proces in
 * Optimize-Transfers-Service. Service starts with egress then leg are continuously appended until
 * access is reached. After each append sequence filter is applied to reduce amount combinations
 * and improve performance.
 */
public interface PathTailFilter<T> {
  /**
   * This filter runs after new legs are appended to the results.
   */
  Set<T> filterIntermediateResult(Set<T> elements);
  /**
   * This is the final filter sequence. Happens after all legs are appended.
   */
  Set<T> filterFinalResult(Set<T> elements);
}
