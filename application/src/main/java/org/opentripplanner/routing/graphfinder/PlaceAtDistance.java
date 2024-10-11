package org.opentripplanner.routing.graphfinder;

/**
 * A place of any of the types defined in PlaceType at a specified distance.
 *
 * @see PlaceType
 */
public record PlaceAtDistance(Object place, double distance) {}
