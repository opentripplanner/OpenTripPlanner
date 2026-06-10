package org.opentripplanner.standalone.config.buildconfig;

import static org.opentripplanner.standalone.config.framework.json.EnumMapper.docEnumValueList;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_10;

import java.net.URI;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.graph_builder.module.cache.CacheTask;
import org.opentripplanner.graph_builder.module.cache.GraphBuildCacheParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

/**
 * Configuration for the graph-build file cache ({@code cache} section of
 * {@code build-config.json}).
 */
public class GraphBuildCacheConfig {

  public final boolean enabled;

  /**
   * Root directory for cache files. {@code null} means "use the OTP base directory".
   */
  @Nullable
  private final URI path;

  /** Which tasks are enabled. Defaults to all known tasks when not set in config. */
  public final Set<CacheTask> tasks;

  private GraphBuildCacheConfig(boolean enabled, @Nullable URI path, Set<CacheTask> tasks) {
    this.enabled = enabled;
    this.path = path;
    this.tasks = Set.copyOf(tasks);
  }

  public static GraphBuildCacheConfig fromConfig(NodeAdapter root) {
    return fromSubConfig(
      root
        .of("cache")
        .since(V2_10)
        .summary("Configuration for the graph-build file cache.")
        .description(
          """
          OTP can cache the results of expensive graph-build computations between builds. Both
          cached tasks can take a significant portion of total graph-build time; enabling the cache
          skips their computation on subsequent builds and can cut build time considerably when the
          same OSM and DEM files are reused.

          **When to enable:** any pipeline that rebuilds the graph repeatedly from the same OSM
          and elevation data (e.g. nightly GTFS-only updates).

          Cache files are named `<task>-cache-<version>.obj` (for example `elevation-cache-1.obj`).
          When OTP bumps the internal serialization version the old file is ignored automatically
          and a new one is written, so old versioned files can be deleted safely at any time.

          Caching is **disabled by default**. Set `enabled: true` to activate it.
          """
        )
        .asObject()
    );
  }

  public static GraphBuildCacheConfig fromSubConfig(NodeAdapter c) {
    var enabled = c
      .of("enabled")
      .since(V2_10)
      .summary("Master switch for the graph-build cache.")
      .description("When `false` no cache files are read or written during graph builds.")
      .asBoolean(false);

    var path = c
      .of("path")
      .since(V2_10)
      .summary("Root directory for cache files.")
      .description(
        """
        Path to the directory where cache files are stored. Defaults to the OTP base directory
        (the directory containing `build-config.json`) when not set.
        """
      )
      .asUri(null);

    var tasks = c
      .of("tasks")
      .since(V2_10)
      .summary(CacheTask.ELEVATION.typeDescription())
      .description(
        docEnumValueList(CacheTask.values()) +
          "\nWhen not set, all tasks are enabled. Omit a task from the list to disable its cache."
      )
      .asEnumSet(CacheTask.class, EnumSet.allOf(CacheTask.class));

    return new GraphBuildCacheConfig(enabled, path, tasks);
  }

  @Nullable
  public URI path(String resourceName) {
    return path == null
      ? URI.create(resourceName)
      : URI.create(path.getPath() + "/" + resourceName);
  }

  public GraphBuildCacheParameters toParameters() {
    return new GraphBuildCacheParameters(enabled, tasks);
  }

  public List<URI> files() {
    return Arrays.stream(CacheTask.values()).map(CacheTask::cacheFileName).map(this::path).toList();
  }
}
