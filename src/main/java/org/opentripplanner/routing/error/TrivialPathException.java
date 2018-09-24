package org.opentripplanner.routing.error;

/**
 * Indicates that origin and destination are too close to one another for effective
 * trip planning.
 */
public class TrivialPathException extends RuntimeException {
    private static final long serialVersionUID = 1L;
}
