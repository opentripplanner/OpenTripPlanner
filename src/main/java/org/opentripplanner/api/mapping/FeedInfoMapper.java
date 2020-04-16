package org.opentripplanner.api.mapping;

import org.opentripplanner.api.model.ApiFeedInfo;
import org.opentripplanner.model.FeedInfo;

public class FeedInfoMapper {
    public static ApiFeedInfo mapToApi(FeedInfo domain) {
        if(domain == null) {
            return null;
        }
        ApiFeedInfo api = new ApiFeedInfo();

        api.id = domain.getId();
        api.publisherName = domain.getPublisherName();
        api.publisherUrl = domain.getPublisherUrl();
        api.lang = domain.getLang();
        api.startDate = ServiceDateMapper.mapToApi(domain.getStartDate());
        api.endDate = ServiceDateMapper.mapToApi(domain.getEndDate());
        api.version = domain.getVersion();

        return api;
    }
}
