package org.opentripplanner.graph_builder.module.cache;

import java.io.Serializable;

/**
 * Wrapper stored as the root object in each cache file. The {@code serializationVersionId} must
 * match the value in {@link CacheTask} for the read to be accepted; a mismatch causes the cache
 * to be treated as a miss.
 */
public class CacheSerializationObject<T> implements Serializable {

  public final int serializationVersionId;
  public final T data;

  public CacheSerializationObject(int serializationVersionId, T data) {
    this.serializationVersionId = serializationVersionId;
    this.data = data;
  }
}
