package org.opentripplanner.ext.restapi.mapping;

import org.opentripplanner.ext.restapi.model.ApiFeedInfo;
import org.opentripplanner.model.FeedInfo;

public class FeedInfoMapper {

  public static ApiFeedInfo mapToApi(FeedInfo domain) {
    if (domain == null) {
      return null;
    }
    ApiFeedInfo api = new ApiFeedInfo();

    api.id = domain.getId();
    api.publisherName = domain.getPublisherName();
    api.publisherUrl = domain.getPublisherUrl();
    api.lang = domain.getLang();
    api.startDate = LocalDateMapper.mapToApi(domain.getStartDate());
    api.endDate = LocalDateMapper.mapToApi(domain.getEndDate());
    api.version = domain.getVersion();

    return api;
  }
}
