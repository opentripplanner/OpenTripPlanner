package org.opentripplanner.transit.transfer.regular.spi;

import java.io.Serializable;
import org.opentripplanner.core.model.id.FeedScopedId;

/**
 * A path {@code P} found by {@link TransferPathProvider#findNearbyStops} together with the stop
 * it arrives at. {@code P} itself stays fully opaque to {@code raptor-data} - {@code toStop} is
 * the one piece of identity the generator needs out of it directly, to store the transfer.
 */
public record NearbyPath<P>(FeedScopedId toStop, P path) implements Serializable {}
