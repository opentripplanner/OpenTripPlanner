package org.opentripplanner.routing.algorithm;

/**
 * This exception is thrown when an edge has a negative weight. Dijkstra's
 * algorithm (and A*) don't work on graphs that have negative weights.  This
 * exception almost always indicates a programming error, but could be 
 * caused by bad GTFS data.
 */
public class NegativeWeightException extends RuntimeException {

    private static final long serialVersionUID = -1018391017439852795L;

    public NegativeWeightException(String message) {
        super(message);
    }

}
