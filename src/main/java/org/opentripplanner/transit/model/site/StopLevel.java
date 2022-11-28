package org.opentripplanner.transit.model.site;

import java.io.Serializable;

/**
 * Immutable value object for stop level. This is currently only supported by the GTFS import.
 */
public record StopLevel(String name, double index) implements Serializable {}
