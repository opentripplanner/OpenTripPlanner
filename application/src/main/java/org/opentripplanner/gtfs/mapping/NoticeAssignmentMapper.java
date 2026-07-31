package org.opentripplanner.gtfs.mapping;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.onebusaway.gtfs.model.NoticeAssignment;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.transit.model.basic.Notice;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.utils.collection.ListUtils;

/**
 * Maps GTFS notice_assignments.txt entries to OTP notice assignments, connecting each
 * {@link Notice} to its target {@link Route}, {@link Trip} or trip segment. A trip segment is
 * expanded to one entry per stop time it covers.
 */
class NoticeAssignmentMapper {

  private final IdFactory idFactory;
  private final DataImportIssueStore issueStore;
  private final NoticeMapper noticeMapper;
  private final RouteMapper routeMapper;
  private final TripMapper tripMapper;
  private final TripSegmentMapper tripSegmentMapper;

  NoticeAssignmentMapper(
    IdFactory idFactory,
    DataImportIssueStore issueStore,
    NoticeMapper noticeMapper,
    TripMapper tripMapper,
    RouteMapper routeMapper,
    TripSegmentMapper tripSegmentMapper
  ) {
    this.idFactory = idFactory;
    this.issueStore = issueStore;
    this.noticeMapper = noticeMapper;
    this.routeMapper = routeMapper;
    this.tripMapper = tripMapper;
    this.tripSegmentMapper = tripSegmentMapper;
  }

  Multimap<AbstractTransitEntity, Notice> map(Collection<NoticeAssignment> assignments) {
    Multimap<AbstractTransitEntity, Notice> result = ArrayListMultimap.create();
    var notices = noticeMapper.mappedNotices();
    var trips = tripMapper
      .getMappedTrips()
      .stream()
      .collect(Collectors.toMap(Trip::getId, Function.identity()));
    var routes = routeMapper.mappedRoutes();
    for (var assignment : assignments) {
      map(assignment, notices, trips, routes).forEach(e -> result.put(e.getKey(), e.getValue()));
    }
    return result;
  }

  private Stream<Map.Entry<AbstractTransitEntity, Notice>> map(
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
      return Stream.of();
    }

    var recordId = idFactory.createId(assignment.getRecordId(), "NoticeAssignment.recordId");

    List<AbstractTransitEntity> entities = switch (assignment.getTableName()) {
      case routes -> ListUtils.ofNullable(routes.get(recordId));
      case trips -> ListUtils.ofNullable(trips.get(recordId));
      case trip_segments -> List.copyOf(tripSegmentMapper.getStopTimeKeys(recordId));
    };

    if (entities.isEmpty()) {
      issueStore.add(
        "NoticeAssignmentWithUnknownEntity",
        "Could not map notice assignment %s for %s with id %s",
        assignment.getId(),
        assignment.getTableName(),
        assignment.getRecordId()
      );
      return Stream.of();
    }

    return entities.stream().map(entity -> Map.entry(entity, notice));
  }
}
