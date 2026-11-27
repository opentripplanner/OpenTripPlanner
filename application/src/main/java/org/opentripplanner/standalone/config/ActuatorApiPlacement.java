package org.opentripplanner.standalone.config;

/**
 * Where the Actuator API (health probes and metrics scraping) is served. It is decided once at
 * startup from the {@code ActuatorAPI} feature and the {@code --port} and {@code --actuatorPort}
 * command line parameters.
 */
public enum ActuatorApiPlacement {
  /** The {@code ActuatorAPI} feature is off, the Actuator API is not served. */
  DISABLED,
  /** Served on the main port, together with the public APIs. */
  MAIN_PORT,
  /**
   * Served only on the {@code --actuatorPort}, isolated from the public APIs and their thread pool.
   */
  SEPARATE_PORT,
}
