package org.opentripplanner.transit.service;

import com.google.common.collect.Multimap;
import java.util.function.Function;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.model.Notice;
import org.opentripplanner.model.TimetableSnapshotProvider;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitLayer;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.transit.model.framework.TransitEntity;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TransitMode;
import org.opentripplanner.transit.model.organization.Agency;

/**
 * Entry point for requests (both read-only and read-write) towards the transit API.
 */
public interface TransitEditorService extends TransitService {
  void addAgency(String feedId, Agency agency);

  void addFeedInfo(FeedInfo info);

  void addNoticeAssignments(Multimap<TransitEntity, Notice> noticesByElement);

  void addRoutes(Route route);

  void addTransitMode(TransitMode mode);

  <T extends TimetableSnapshotProvider> T getOrSetupTimetableSnapshotProvider(
    Function<Graph, T> creator
  );

  void setTransitLayer(TransitLayer transitLayer);
}
