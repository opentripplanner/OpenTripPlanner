package org.opentripplanner.routing.error;

/**
 * Indicates that the call to org.opentripplanner.routing.services.PathService returned either null or ZERO paths.
 * @see org.opentripplanner.api.ws.Planner for where this is (locally) thrown.
 */
public class PathNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;
}
