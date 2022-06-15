package org.opentripplanner.transit.service;

import com.google.common.collect.Multimap;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.model.Notice;
import org.opentripplanner.transit.model.basic.TransitEntity;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Agency;

public interface TransitEditorService extends TransitService {
  void addAgency(String feedId, Agency agency);

  void addFeedInfo(FeedInfo info);

  void addNoticeAssignments(Multimap<TransitEntity, Notice> noticesByElement);

  void addRoutes(Route route);
}
