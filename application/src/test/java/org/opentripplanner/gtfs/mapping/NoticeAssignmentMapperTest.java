package org.opentripplanner.gtfs.mapping;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.onebusaway.gtfs.model.AgencyAndIdFactory.obaId;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.onebusaway.gtfs.model.NoticeAssignment;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issue.service.DefaultDataImportIssueStore;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;

class NoticeAssignmentMapperTest {

  private static final String FEED_ID = TimetableRepositoryForTest.FEED_ID;
  private static final IdFactory ID_FACTORY = new IdFactory(FEED_ID);

  private static final String NOTICE_ID = "N1";
  private static final String NOTICE_TEXT = "Platform change";

  private static final GtfsTestData DATA = new GtfsTestData();

  private static final org.onebusaway.gtfs.model.Notice GTFS_NOTICE;

  static {
    GTFS_NOTICE = new org.onebusaway.gtfs.model.Notice();
    GTFS_NOTICE.setId(obaId(NOTICE_ID));
    GTFS_NOTICE.setDisplayText(NOTICE_TEXT);
  }

  private static RouteMapper createRouteMapper() {
    return new RouteMapper(
      ID_FACTORY,
      new AgencyMapper(ID_FACTORY),
      new RouteNetworkAssignmentMapper(ID_FACTORY),
      DataImportIssueStore.NOOP,
      new TranslationHelper()
    );
  }

  private static TripMapper createTripMapper(RouteMapper routeMapper) {
    return new TripMapper(
      ID_FACTORY,
      routeMapper,
      new DirectionMapper(DataImportIssueStore.NOOP),
      new TranslationHelper()
    );
  }

  @Test
  void mapNoticeAssignmentOnRoute() {
    var routeMapper = createRouteMapper();
    var route = routeMapper.map(DATA.route);

    var noticeMapper = new NoticeMapper(ID_FACTORY);
    noticeMapper.map(GTFS_NOTICE);

    var mapper = new NoticeAssignmentMapper(
      ID_FACTORY,
      DataImportIssueStore.NOOP,
      noticeMapper,
      createTripMapper(routeMapper),
      routeMapper
    );

    var assignment = new NoticeAssignment();
    assignment.setNoticeId(obaId(NOTICE_ID));
    assignment.setTableName(NoticeAssignment.TableName.routes);
    assignment.setRecordId(obaId("R1"));

    var result = mapper.map(List.of(assignment));

    assertEquals(1, result.size());
    var notice = result.get(route).iterator().next();
    assertEquals(NOTICE_ID, notice.getId().getId());
    assertEquals(NOTICE_TEXT, notice.text());
  }

  @Test
  void mapNoticeAssignmentOnTrip() {
    var routeMapper = createRouteMapper();
    var tripMapper = createTripMapper(routeMapper);
    var trip = tripMapper.map(DATA.trip);

    var noticeMapper = new NoticeMapper(ID_FACTORY);
    noticeMapper.map(GTFS_NOTICE);

    var mapper = new NoticeAssignmentMapper(
      ID_FACTORY,
      DataImportIssueStore.NOOP,
      noticeMapper,
      tripMapper,
      routeMapper
    );

    var assignment = new NoticeAssignment();
    assignment.setNoticeId(obaId(NOTICE_ID));
    assignment.setTableName(NoticeAssignment.TableName.trips);
    assignment.setRecordId(obaId("T1"));

    var result = mapper.map(List.of(assignment));

    assertEquals(1, result.size());
    var notice = result.get(trip).iterator().next();
    assertEquals(NOTICE_ID, notice.getId().getId());
  }

  @Test
  void missingNoticeLogsIssue() {
    var issues = new DefaultDataImportIssueStore();
    var noticeMapper = new NoticeMapper(ID_FACTORY);
    var routeMapper = createRouteMapper();

    var mapper = new NoticeAssignmentMapper(
      ID_FACTORY,
      issues,
      noticeMapper,
      createTripMapper(routeMapper),
      routeMapper
    );

    var assignment = new NoticeAssignment();
    assignment.setNoticeId(obaId("NONEXISTENT"));
    assignment.setTableName(NoticeAssignment.TableName.routes);
    assignment.setRecordId(obaId("R1"));

    var result = mapper.map(List.of(assignment));

    assertThat(result).isEmpty();
    assertThat(issues.listIssues().stream().map(DataImportIssue::getType)).containsExactly(
      "NoticeAssignmentWithoutNotice"
    );
  }

  @Test
  void missingRouteEntityLogsIssue() {
    var issues = new DefaultDataImportIssueStore();
    var noticeMapper = new NoticeMapper(ID_FACTORY);
    noticeMapper.map(GTFS_NOTICE);
    var routeMapper = createRouteMapper();

    var mapper = new NoticeAssignmentMapper(
      ID_FACTORY,
      issues,
      noticeMapper,
      createTripMapper(routeMapper),
      routeMapper
    );

    var assignment = new NoticeAssignment();
    assignment.setNoticeId(obaId(NOTICE_ID));
    assignment.setTableName(NoticeAssignment.TableName.routes);
    assignment.setRecordId(obaId("NONEXISTENT"));

    var result = mapper.map(List.of(assignment));

    assertThat(result).isEmpty();
    assertThat(issues.listIssues().stream().map(DataImportIssue::getType).toList()).containsExactly(
      "NoticeAssignmentWithUnknownEntity"
    );
  }

  @Test
  void missingTripEntityLogsIssue() {
    var issues = new DefaultDataImportIssueStore();
    var noticeMapper = new NoticeMapper(ID_FACTORY);
    noticeMapper.map(GTFS_NOTICE);
    var routeMapper = createRouteMapper();

    var mapper = new NoticeAssignmentMapper(
      ID_FACTORY,
      issues,
      noticeMapper,
      createTripMapper(routeMapper),
      routeMapper
    );

    var assignment = new NoticeAssignment();
    assignment.setNoticeId(obaId(NOTICE_ID));
    assignment.setTableName(NoticeAssignment.TableName.trips);
    assignment.setRecordId(obaId("NONEXISTENT"));

    var result = mapper.map(List.of(assignment));

    assertThat(result).isEmpty();
    assertThat(issues.listIssues().stream().map(DataImportIssue::getType).toList()).containsExactly(
      "NoticeAssignmentWithUnknownEntity"
    );
  }

  @Test
  void multipleAssignmentsMappedTogether() {
    var routeMapper = createRouteMapper();
    var tripMapper = createTripMapper(routeMapper);

    var route = routeMapper.map(DATA.route);
    var trip = tripMapper.map(DATA.trip);

    var noticeMapper = new NoticeMapper(ID_FACTORY);
    noticeMapper.map(GTFS_NOTICE);

    var mapper = new NoticeAssignmentMapper(
      ID_FACTORY,
      DataImportIssueStore.NOOP,
      noticeMapper,
      tripMapper,
      routeMapper
    );

    var routeAssignment = new NoticeAssignment();
    routeAssignment.setNoticeId(obaId(NOTICE_ID));
    routeAssignment.setTableName(NoticeAssignment.TableName.routes);
    routeAssignment.setRecordId(obaId("R1"));

    var tripAssignment = new NoticeAssignment();
    tripAssignment.setNoticeId(obaId(NOTICE_ID));
    tripAssignment.setTableName(NoticeAssignment.TableName.trips);
    tripAssignment.setRecordId(obaId("T1"));

    var result = mapper.map(List.of(routeAssignment, tripAssignment));

    assertEquals(2, result.size());
    assertEquals(1, result.get(route).size());
    assertEquals(1, result.get(trip).size());
  }
}
