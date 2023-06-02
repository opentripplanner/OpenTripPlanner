package org.opentripplanner.routing.algorithm.transferoptimization.model;

import java.util.Set;

public interface OptimizeTransferFilterChain<T> {
  public Set<T> filter(Set<T> elements);
}
