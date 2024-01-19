package org.opentripplanner.routing.error;

/**
 * Indicates that the call to org.opentripplanner.routing.services.PathService returned either null
 * or ZERO paths.
 *
 */
public class PathNotFoundException extends RuntimeException {}
