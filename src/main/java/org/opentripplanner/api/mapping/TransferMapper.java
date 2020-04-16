package org.opentripplanner.api.mapping;

import org.opentripplanner.api.model.ApiTransfer;
import org.opentripplanner.model.SimpleTransfer;

public class TransferMapper {
    /** Make a transfer from a simpletransfer edge from the graph. */
    public static ApiTransfer mapToApi(SimpleTransfer domain) {
        if(domain == null) {
            return null;
        }
        ApiTransfer api = new ApiTransfer();
        api.toStopId = FeedScopedIdMapper.mapToApi(domain.to.getId());
        api.distance = domain.getDistanceMeters();
        return api;
    }
}
