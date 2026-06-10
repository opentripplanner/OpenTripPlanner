package org.opentripplanner.graph_builder.module.cache;

import java.util.Set;

/**
 * Runtime parameters for the graph-build cache, derived from {@code build-config.json}.
 * Kept in the cache package so that {@link GraphBuildCacheManager} and related classes have
 * no dependency on the configuration layer.
 */
public record GraphBuildCacheParameters(boolean enabled, Set<CacheTask> tasks) {
  /** Disabled instance — safe to use as a default when no real cache is configured. */
  public static final GraphBuildCacheParameters DISABLED = new GraphBuildCacheParameters(
    false,
    Set.of()
  );

  public GraphBuildCacheParameters {
    tasks = Set.copyOf(tasks);
  }

  public boolean isEnabled(CacheTask task) {
    return enabled && tasks.contains(task);
  }
}
