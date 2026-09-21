package org.opentripplanner.graph_builder.module.cache;

import org.opentripplanner.core.model.doc.DocumentedEnum;

/**
 * Identifies a graph-build computation that can be persisted to a file cache between builds.
 * Each task carries its own serialization version ID, maintained manually in code and independent
 * of the global OTP serialization version. Bump the ID when the cache file format changes for
 * that specific task.
 */
public enum CacheTask implements DocumentedEnum<CacheTask> {
  ELEVATION(1),
  VISIBILITY(1);

  /**
   * Per-task cache format version. Increment when the serialized data structure changes so that
   * stale files are ignored automatically (the version is part of the filename).
   */
  public final int serializationVersionId;

  CacheTask(int serializationVersionId) {
    this.serializationVersionId = serializationVersionId;
  }

  public String cacheFileName() {
    return name().toLowerCase() + "-cache-" + serializationVersionId + ".obj";
  }

  @Override
  public String typeDescription() {
    return "Which graph-build computations to cache between builds.";
  }

  @Override
  public String enumValueDescription() {
    return switch (this) {
      case ELEVATION -> """
      Caches the elevation profile sampled for every street edge. The cache key is the encoded edge
      geometry; OSM changes are handled automatically (new or modified edges cause a cache miss,
      removed edges are omitted from the next save).

      **Delete the elevation cache when the DEM source file is replaced**, because edge geometries
      are unchanged but the sampled height values would be stale.""";
      case VISIBILITY -> """
      Caches pre-computed visibility graphs for walkable OSM areas (parks, plazas, etc.). The cache
      key is a hash of the OSM entity IDs and all polygon coordinates, so any geometry change
      causes a cache miss automatically. Only entries accessed during a build are written back, so
      deleted areas are pruned from the saved file automatically.

      **The visibility cache never needs to be deleted manually.**""";
    };
  }

  public static CacheTask matchName(String name) {
    for (var it : values()) {
      if (it.cacheFileName().equals(name)) {
        return it;
      }
    }
    return null;
  }
}
