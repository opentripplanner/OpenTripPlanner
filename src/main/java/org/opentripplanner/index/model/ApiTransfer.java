package org.opentripplanner.index.model;

import org.opentripplanner.api.mapping.FeedScopedIdMapper;
import org.opentripplanner.model.SimpleTransfer;

/** Represents a transfer from a stop */
public class ApiTransfer {
    /** The stop we are connecting to */
    public String toStopId;

    /** the on-street distance of the transfer (meters) */
    public double distance;

    /** Make a transfer from a simpletransfer edge from the graph. */
    public ApiTransfer(SimpleTransfer e) {
        toStopId = FeedScopedIdMapper.mapToApi(e.to.getId());
        distance = e.getDistanceMeters();
    }
}
