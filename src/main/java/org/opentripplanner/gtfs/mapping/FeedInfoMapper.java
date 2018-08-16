package org.opentripplanner.gtfs.mapping;

import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.util.MapUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.opentripplanner.gtfs.mapping.ServiceDateMapper.mapServiceDate;

/** Responsible for mapping GTFS FeedInfo into the OTP model. */
class FeedInfoMapper {
    private Map<org.onebusaway.gtfs.model.FeedInfo, FeedInfo> mappedFeedInfos = new HashMap<>();

    Collection<FeedInfo> map(Collection<org.onebusaway.gtfs.model.FeedInfo> feedInfos) {
        return feedInfos == null ? null : MapUtils.mapToList(feedInfos, this::map);
    }

    /** Map from GTFS to OTP model, {@code null} safe.  */
    FeedInfo map(org.onebusaway.gtfs.model.FeedInfo orginal) {
        return orginal == null ? null : mappedFeedInfos.computeIfAbsent(orginal, this::doMap);
    }

    private FeedInfo doMap(org.onebusaway.gtfs.model.FeedInfo rhs) {
        FeedInfo lhs = new FeedInfo();

        lhs.setId(rhs.getId());
        lhs.setPublisherName(rhs.getPublisherName());
        lhs.setPublisherUrl(rhs.getPublisherUrl());
        lhs.setLang(rhs.getLang());
        lhs.setStartDate(mapServiceDate(rhs.getStartDate()));
        lhs.setEndDate(mapServiceDate(rhs.getEndDate()));
        lhs.setVersion(rhs.getVersion());

        return lhs;
    }
}
