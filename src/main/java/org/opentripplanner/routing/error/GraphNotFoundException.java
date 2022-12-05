package org.opentripplanner.routing.error;

/**
 * Indicates that there is no graph currently available. This might be thrown if a new graph is
 * registered and the old one is evicted before registering.
 */
public class GraphNotFoundException extends RuntimeException {}
