package org.opentripplanner.ext.fares.model;

/**
 * The use of a fare product that might be shared across several legs.
 * <p>
 * The id identifies the fare product across the legs.
 */
public record FareProductUse(String id, FareProduct product) {}
