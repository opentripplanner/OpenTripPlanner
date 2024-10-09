package org.opentripplanner.model.fare;

import org.opentripplanner.framework.lang.Sandbox;

/**
 *
 * The use of a fare product that might be shared across several legs.
 * <p>
 * The id identifies the fare product across the legs.
 */
@Sandbox
public record FareProductUse(String id, FareProduct product) {}
