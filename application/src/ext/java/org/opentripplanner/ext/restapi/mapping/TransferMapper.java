package org.opentripplanner.ext.restapi.mapping;

import org.opentripplanner.ext.restapi.model.ApiTransfer;
import org.opentripplanner.model.PathTransfer;

public class TransferMapper {

  public static ApiTransfer mapToApi(PathTransfer domain) {
    if (domain == null) {
      return null;
    }
    ApiTransfer api = new ApiTransfer();
    api.toStopId = FeedScopedIdMapper.mapToApi(domain.to.getId());
    api.distance = domain.getDistanceMeters();
    return api;
  }
}
