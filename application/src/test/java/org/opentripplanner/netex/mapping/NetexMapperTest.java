package org.opentripplanner.netex.mapping;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.graph_builder.issue.service.DefaultDataImportIssueStore;
import org.opentripplanner.model.impl.TransitDataImportBuilder;
import org.opentripplanner.netex.index.NetexEntityIndex;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.SiteRepository;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.JourneyPatternRefStructure;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.LineRefStructure;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.Notice;
import org.rutebanken.netex.model.NoticeAssignment;
import org.rutebanken.netex.model.PointInLinkSequence_VersionedChildStructure;
import org.rutebanken.netex.model.PointsInJourneyPattern_RelStructure;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.Route;
import org.rutebanken.netex.model.RouteRefStructure;
import org.rutebanken.netex.model.ScheduledStopPointRefStructure;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.StopPointInJourneyPattern;
import org.rutebanken.netex.model.StopPointInJourneyPatternRefStructure;
import org.rutebanken.netex.model.TimetabledPassingTime;
import org.rutebanken.netex.model.TimetabledPassingTimes_RelStructure;
import org.rutebanken.netex.model.VersionOfObjectRefStructure;

class NetexMapperTest {

  private static final String QUAY_ID = "quay-1";
  private static final String SSP_ID = "ssp-1";
  private static final String FEED_ID = "sta";
  private static final RegularStop STOP = stop(QUAY_ID);
  private static final Deduplicator DEDUPLICATOR = new Deduplicator();
  private static final String TIMETABLED_PASSING_TIME_ID = "TTPT-1";

  @Test
  void sspWithAssignment() {
    var issueStore = new DefaultDataImportIssueStore();
    var transitBuilder = new TransitDataImportBuilder(SiteRepository.of().build(), issueStore);
    transitBuilder.siteRepository().withRegularStop(STOP);

    var netexMapper = new NetexMapper(
      transitBuilder,
      FEED_ID,
      DEDUPLICATOR,
      issueStore,
      Set.of(),
      Set.of(),
      10,
      false
    );

    var index = new NetexEntityIndex();
    index.quayById.add(new Quay().withId(QUAY_ID));
    index.quayIdByStopPointRef.add(SSP_ID, QUAY_ID);
    netexMapper.mapNetexToOtp(index.readOnlyView());

    assertEquals(
      STOP,
      transitBuilder.stopsByScheduledStopPoints().get(new FeedScopedId(FEED_ID, SSP_ID))
    );
  }

  @Test
  void sspPointsToUnknownId() {
    var issueStore = new DefaultDataImportIssueStore();

    var netexMapper = new NetexMapper(
      new TransitDataImportBuilder(SiteRepository.of().build(), issueStore),
      FEED_ID,
      DEDUPLICATOR,
      issueStore,
      Set.of(),
      Set.of(),
      10,
      false
    );

    var index = new NetexEntityIndex();
    index.quayById.add(new Quay().withId(QUAY_ID));
    index.quayIdByStopPointRef.add(SSP_ID, QUAY_ID);
    netexMapper.mapNetexToOtp(index.readOnlyView());

    var issueTypes = issueStore.listIssues().stream().map(DataImportIssue::getType).toList();

    assertThat(issueTypes).contains("ScheduledStopPointAssignedToUnknownQuay");
  }

  /**
   * {@link org.opentripplanner.netex.mapping.support.NetexMapperIndexes#getStopTimesByNetexId()}
   * is documented to be scoped per hierarchy level (shared files, shared-group files, group
   * files) and thrown away once that level is popped.
   */
  @Test
  void noticeAssignmentIsNotResolvedAcrossSiblingFiles() {
    var issueStore = new DefaultDataImportIssueStore();
    var transitBuilder = new TransitDataImportBuilder(SiteRepository.of().build(), issueStore);
    transitBuilder.siteRepository().withRegularStop(stop("quay-1"));
    transitBuilder.siteRepository().withRegularStop(stop("quay-2"));

    var netexMapper = new NetexMapper(
      transitBuilder,
      FEED_ID,
      DEDUPLICATOR,
      issueStore,
      Set.of(),
      Set.of(),
      10,
      false
    );

    // Level 0 ("shared data"): empty, but it is the common ancestor for both group files below.
    netexMapper.mapNetexToOtp(new NetexEntityIndex().readOnlyView());

    // Level 1 ("group"), file A: maps a ServiceJourney with a TimetabledPassingTime whose id is
    // TIMETABLED_PASSING_TIME_ID.
    netexMapper.push();
    netexMapper.mapNetexToOtp(fileWithServiceJourney().readOnlyView());
    netexMapper.pop();

    // Level 1, file B: a sibling of file A (not a descendant of it), whose NoticeAssignment
    // refers to the TimetabledPassingTime mapped while processing file A.
    netexMapper.push();
    netexMapper.mapNetexToOtp(fileWithNoticeAssignment(TIMETABLED_PASSING_TIME_ID).readOnlyView());
    netexMapper.pop();

    // If file A's StopTime had leaked into file B's lookup (the bug this guards against), the
    // NoticeAssignment would have resolved successfully and none of these issues would be raised.
    var issueTypes = issueStore.listIssues().stream().map(DataImportIssue::getType).toList();
    assertThat(issueTypes).contains("NoticeAssignmentWithUnknownEntity");
  }

  private static RegularStop stop(String quayId) {
    return RegularStop.of(new FeedScopedId(FEED_ID, quayId), () -> 1)
      .withCoordinate(60.0, 10.0)
      .build();
  }

  private static NetexEntityIndex fileWithServiceJourney() {
    var index = new NetexEntityIndex();
    index.timeZone.set("Europe/Oslo");

    Line line = new Line()
      .withId("RUT:Line:1")
      .withName(new MultilingualString().withValue("Line 1"))
      .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS);
    index.lineById.add(line);

    var lineRef = MappingSupport.createWrappedRef(line.getId(), LineRefStructure.class);
    index.routeById.add(new Route().withId("RUT:Route:1").withLineRef(lineRef));
    var routeRef = new RouteRefStructure().withRef("RUT:Route:1");

    List<PointInLinkSequence_VersionedChildStructure> points = new ArrayList<>();
    List<TimetabledPassingTime> passingTimes = new ArrayList<>();
    String[] quayIds = { "quay-1", "quay-2" };
    String[] passingTimeIds = { TIMETABLED_PASSING_TIME_ID, "TTPT-2" };

    for (int i = 0; i < quayIds.length; i++) {
      String stopPointId = "RUT:StopPointInJourneyPattern:" + (i + 1);
      points.add(
        new StopPointInJourneyPattern()
          .withId(stopPointId)
          .withOrder(BigInteger.valueOf(i + 1))
          .withScheduledStopPointRef(
            MappingSupport.createWrappedRef(stopPointId, ScheduledStopPointRefStructure.class)
          )
      );
      passingTimes.add(
        new TimetabledPassingTime()
          .withId(passingTimeIds[i])
          .withDepartureTime(LocalTime.of(8, i))
          .withPointInJourneyPatternRef(
            MappingSupport.createWrappedRef(
              stopPointId,
              StopPointInJourneyPatternRefStructure.class
            )
          )
      );
      index.quayIdByStopPointRef.add(stopPointId, quayIds[i]);
    }

    var journeyPattern = new JourneyPattern()
      .withId("RUT:JourneyPattern:1")
      .withRouteRef(routeRef)
      .withPointsInSequence(
        new PointsInJourneyPattern_RelStructure().withPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern(
          points
        )
      );
    index.journeyPatternsById.add(journeyPattern);

    var serviceJourney = new ServiceJourney()
      .withId("RUT:ServiceJourney:1")
      .withLineRef(lineRef)
      .withJourneyPatternRef(
        MappingSupport.createWrappedRef(journeyPattern.getId(), JourneyPatternRefStructure.class)
      )
      .withPassingTimes(
        new TimetabledPassingTimes_RelStructure().withTimetabledPassingTime(passingTimes)
      );
    index.serviceJourneyById.add(serviceJourney);

    return index;
  }

  private static NetexEntityIndex fileWithNoticeAssignment(String noticedObjectId) {
    var index = new NetexEntityIndex();
    var notice = new Notice()
      .withId("RUT:Notice:1")
      .withText(new MultilingualString().withValue("Notice text"));

    var noticeAssignment = new NoticeAssignment()
      .withId("RUT:NoticeAssignment:1")
      .withNoticedObjectRef(new VersionOfObjectRefStructure().withRef(noticedObjectId))
      .withNotice(notice);
    index.noticeAssignmentById.add(noticeAssignment);

    return index;
  }
}
