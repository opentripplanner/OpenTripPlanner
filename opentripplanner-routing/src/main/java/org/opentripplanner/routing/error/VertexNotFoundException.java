package org.opentripplanner.routing.error;

import java.util.List;

/**
 * Indicates that a vertex requested by name or lat/long could not be located.
 * This might be thrown if a user enters a location outside the street/transit network.
 */
public class VertexNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    List<String> notFound;
    
    public VertexNotFoundException(List<String> notFound) {
        this.notFound = notFound;
    }

    public List<String> getMissing() {
        return notFound;
    }
    
}
