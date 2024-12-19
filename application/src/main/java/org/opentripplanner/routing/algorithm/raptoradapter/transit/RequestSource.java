package org.opentripplanner.routing.algorithm.raptoradapter.transit;

public enum RequestSource {
  /**
   * The request comes from transferCacheRequests in router-config.json
   */
  CONFIG,
  /**
   * The request comes from a client routing request
   */
  RUNTIME,
}
