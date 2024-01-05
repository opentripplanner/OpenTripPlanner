package org.opentripplanner.inspector.vector;

import org.opentripplanner.apis.support.mapping.PropertyMapper;

/**
 * Configuration options for a single vector tile layer.
 */
public interface LayerParameters<T extends Enum<T>> {
  int MIN_ZOOM = 9;
  int MAX_ZOOM = 20;
  int CACHE_MAX_SECONDS = -1;
  double EXPANSION_FACTOR = 0.25d;

  /**
   * User-visible name of the layer
   */
  String name();

  /**
   * Which {@link LayerBuilder} to use for fetching the geometries/objects for the layer.
   */
  T type();

  /**
   * Which {@link PropertyMapper} to use for mapping the object to the output properties.
   */
  String mapper();

  /**
   * Which is the maximum zoom level the layer should be visible on.
   */
  default int maxZoom() {
    return MAX_ZOOM;
  }

  /**
   * Which is the minimum zoom level the layer should be visible on.
   */
  default int minZoom() {
    return MIN_ZOOM;
  }

  /**
   * How long should the clients cache the response. -1 disables caching altogether.
   */
  default int cacheMaxSeconds() {
    return CACHE_MAX_SECONDS;
  }

  /**
   * How much larger area, than the map tile should be used for fetching objects on the layer.
   * See <a href="https://blog.cyclemap.link/2020-01-25-tilebuffer/">this</a> for more details.
   */
  default double expansionFactor() {
    return EXPANSION_FACTOR;
  }
}
