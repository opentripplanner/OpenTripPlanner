package org.opentripplanner.routing.algorithm.transferoptimization.model;

import java.util.Set;

/**
 * TODO PT - This need JavaDoc
 */
public interface PathTailFilter<T> {
  /**
   * TODO PT - This need JavaDoc
   */
  Set<T> filterIntermediateResult(Set<T> elements);
  /**
   * TODO PT - This need JavaDoc
   */
  Set<T> filterFinalResult(Set<T> elements);
}
