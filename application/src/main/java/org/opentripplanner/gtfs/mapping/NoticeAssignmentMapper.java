package org.opentripplanner.gtfs.mapping;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.onebusaway.gtfs.model.NoticeAssignment;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.transit.model.basic.Notice;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.timetable.Trip;

/**
 * Maps GTFS notice_assignments.txt entries to OTP notice assignments, connecting each
 * {@link Notice} to its target {@link Route} or {@link Trip}.
 */
class NoticeAssignmentMapper {

  private final IdFactory idFactory;
  private final DataImportIssueStore issueStore;
  private final NoticeMapper noticeMapper;
  private final RouteMapper routeMapper;
  private final TripMapper tripMapper;

  NoticeAssignmentMapper(
    IdFactory idFactory,
    DataImportIssueStore issueStore,
    NoticeMapper noticeMapper,
    TripMapper tripMapper,
    RouteMapper routeMapper
  ) {
    this.idFactory = idFactory;
    this.issueStore = issueStore;
    this.noticeMapper = noticeMapper;
    this.routeMapper = routeMapper;
    this.tripMapper = tripMapper;
  }

  Multimap<AbstractTransitEntity, Notice> map(Collection<NoticeAssignment> assignments) {
    Multimap<AbstractTransitEntity, Notice> result = ArrayListMultimap.create();
    var trips = tripMapper
      .getMappedTrips()
      .stream()
      .collect(Collectors.toMap(Trip::getId, Function.identity()));
    var routes = routeMapper.mappedRoutes();
    for (var assignment : assignments) {
      mapOne(assignment, noticeMapper.mappedNotices(), trips, routes).ifPresent(entry ->
        result.put(entry.getKey(), entry.getValue())
      );
    }
    return result;
  }

  private Optional<Map.Entry<AbstractTransitEntity, Notice>> mapOne(
    NoticeAssignment assignment,
    Map<FeedScopedId, Notice> notices,
    Map<FeedScopedId, Trip> trips,
    Map<FeedScopedId, Route> routes
  ) {
    var notice = notices.get(
      idFactory.createId(assignment.getNoticeId(), "notice_id in notice assignment")
    );
    if (notice == null) {
      issueStore.add(
        "NoticeAssignmentWithoutNotice",
        "Notice in notice assignment is missing for assignment %s",
        assignment
      );
      return Optional.empty();
    }

    var recordId = idFactory.createId(assignment.getRecordId(), "NoticeAssignment.recordId");

    AbstractTransitEntity entity = switch (assignment.getTableName()) {
      case routes -> routes.get(recordId);
      case trips -> trips.get(recordId);
      case trip_segments -> null;
    };

    if (entity == null) {
      issueStore.add(
        "NoticeAssignmentWithUnknownEntity",
        "Could not map notice assignment %s for %s with id %s",
        assignment.getId(),
        assignment.getTableName(),
        assignment.getRecordId()
      );
      return Optional.empty();
    }

    return Optional.of(Map.entry(entity, notice));
  }
}
