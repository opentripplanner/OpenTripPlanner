package org.opentripplanner.transit.transfer.regular.spi;

/**
 * The cost and duration of a path under a specific {@code (profileId, preferences)}. {@code c1} is in
 * Raptor cost units (centi-seconds), matching {@link org.opentripplanner.raptor.spi.RaptorTransfer#c1()}.
 */
public record PathCriteria(int c1, int durationInSeconds) {}
