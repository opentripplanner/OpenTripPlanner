package org.opentripplanner.transit.transfer.regular.spi;

import org.opentripplanner.core.model.id.FeedScopedId;

import java.io.Serializable;

/**
 * A path {@code P} found by {@link TransferPathProvider#findNearbyStops} together with the stop
 * it arrives at. {@code P} itself stays fully opaque to {@code raptor-data} - the target stop is
 * the one piece of identity the generator needs out of it directly, to store the transfer.
 */
public record NearbyPath<P>(FeedScopedId targetStop, P path) implements Serializable {}
