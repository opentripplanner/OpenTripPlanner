package org.opentripplanner.ext.fares.model;

/**
 * An instance of a fare product that might be shared across several legs.
 * <p>
 * The instance id identifies the fare product across the legs.
 */
public record FareProductInstance(String instanceId, FareProduct product) {}
