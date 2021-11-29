package org.opentripplanner.api.mapping;

import org.opentripplanner.api.model.ApiTransfer;
import org.opentripplanner.model.PathTransfer;

public class TransferMapper {

    public static ApiTransfer mapToApi(PathTransfer domain) {
        if(domain == null) {
            return null;
        }
        ApiTransfer api = new ApiTransfer();
        api.toStopId = FeedScopedIdMapper.mapToApi(domain.to.getId());
        api.distance = domain.getDistanceMeters();
        return api;
    }
}
