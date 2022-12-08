package org.opentripplanner.gtfs.mapping;

import static org.opentripplanner.gtfs.mapping.ServiceDateMapper.mapLocalDate;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.framework.collection.MapUtils;
import org.opentripplanner.model.FeedInfo;

/** Responsible for mapping GTFS FeedInfo into the OTP model. */
class FeedInfoMapper {

  private final Map<org.onebusaway.gtfs.model.FeedInfo, FeedInfo> mappedFeedInfos = new HashMap<>();

  private final String feedId;

  FeedInfoMapper(String feedId) {
    this.feedId = feedId;
  }

  Collection<FeedInfo> map(Collection<org.onebusaway.gtfs.model.FeedInfo> feedInfos) {
    return feedInfos == null ? null : MapUtils.mapToList(feedInfos, this::map);
  }

  /** Map from GTFS to OTP model, {@code null} safe. */
  FeedInfo map(org.onebusaway.gtfs.model.FeedInfo orginal) {
    return orginal == null ? null : mappedFeedInfos.computeIfAbsent(orginal, this::doMap);
  }

  private FeedInfo doMap(org.onebusaway.gtfs.model.FeedInfo rhs) {
    return new FeedInfo(
      feedId,
      rhs.getPublisherName(),
      rhs.getPublisherUrl(),
      rhs.getLang(),
      mapLocalDate(rhs.getStartDate()),
      mapLocalDate(rhs.getEndDate()),
      rhs.getVersion()
    );
  }
}
