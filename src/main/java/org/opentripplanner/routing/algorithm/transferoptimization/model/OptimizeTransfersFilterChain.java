package org.opentripplanner.routing.algorithm.transferoptimization.model;

import java.util.Set;

public interface OptimizeTransfersFilterChain<T> {
  Set<T> filter(Set<T> elements);
}
