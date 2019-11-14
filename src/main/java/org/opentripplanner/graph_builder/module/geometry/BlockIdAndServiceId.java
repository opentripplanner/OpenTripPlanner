package org.opentripplanner.graph_builder.module.geometry;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Trip;

/**
 * This compound key object is used when grouping interlining trips together by (serviceId, blockId).
 */
class BlockIdAndServiceId {
    String blockId;
    FeedScopedId serviceId;

    BlockIdAndServiceId(Trip trip) {
        this.blockId = trip.getBlockId();
        this.serviceId = trip.getServiceId();
    }

    public boolean equals(Object o) {
        if (o instanceof BlockIdAndServiceId) {
            BlockIdAndServiceId other = ((BlockIdAndServiceId) o);
            return other.blockId.equals(blockId) && other.serviceId.equals(serviceId);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return blockId.hashCode() * 31 + serviceId.hashCode();
    }
}
