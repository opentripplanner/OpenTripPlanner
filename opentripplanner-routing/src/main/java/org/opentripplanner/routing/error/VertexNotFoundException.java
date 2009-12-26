package org.opentripplanner.routing.error;

/**
 * Indicates that a vertex requested by name or lat/long could not be located.
 * This might be thrown if a user enters a location outside the street/transit network.
 */
public class VertexNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /** The departure vertex is missing */
    public boolean fromMissing;
    
    /** The arrival vertex is missing */
    public boolean toMissing;
    
    public VertexNotFoundException(boolean fromMissing, boolean toMissing) {
        this.fromMissing = fromMissing;
        this.toMissing = toMissing;
    }
    
}
