package org.opentripplanner.ext.taxizone.graphbuilder;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.core.model.id.FeedScopedIdForTestFactory;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.ext.flex.trip.ScheduledDeviatedTrip;
import org.opentripplanner.ext.flex.trip.UnscheduledTrip;
import org.opentripplanner.graph_builder.issue.service.DefaultDataImportIssueStore;
import org.opentripplanner.model.FlexStopTimesFactory;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.model.timetable.Trip;

class TaxiZoneBuilderTest {

  private static final TransitRepositoryForTest TEST_MODEL = TransitRepositoryForTest.of();

  private static final AreaStop AREA_1 = TEST_MODEL.areaStop("area-1").build();
  private static final AreaStop AREA_2 = TEST_MODEL.areaStop("area-2").build();

  private static final Trip TRIP = TransitRepositoryForTest.trip("car-pickup")
    .withRoute(TransitRepositoryForTest.route("taxi-route").withMode(TransitMode.TAXI).build())
    .withServiceId(FeedScopedIdForTestFactory.id("service-1"))
    .build();

  @Test
  void validTripProducesZone() {
    var issueStore = new DefaultDataImportIssueStore();
    var trip = unscheduledTrip(validStopTimes());
    var zones = new TaxiZoneBuilder(issueStore).buildZones(List.of(trip));

    assertThat(zones).hasSize(1);
    var zone = zones.get(0);
    assertThat(zone.geometry()).isEqualTo(AREA_1.getGeometry());
    assertThat(zone.route()).isEqualTo(TRIP.getRoute());
    assertThat(issueStore.listIssues()).isEmpty();
  }

  @Test
  void scheduledTripIsSkipped() {
    var issueStore = new DefaultDataImportIssueStore();
    var stopTimes = List.of(
      FlexStopTimesFactory.area(AREA_1, "10:10", "10:15"),
      FlexStopTimesFactory.regularStop("10:40", "10:45")
    );
    var tripId = FeedScopedIdForTestFactory.id("t2");
    var trip = ScheduledDeviatedTrip.of(tripId).withStopTimes(stopTimes).build();

    var zones = new TaxiZoneBuilder(issueStore).buildZones(List.of(trip));

    assertThat(zones).isEmpty();
    assertSingleTaxiZoneTripSkippedIssue(issueStore, tripId, "only UnscheduledTrip is supported");
  }

  @Test
  void nonTaxiRouteTypeIsSkipped() {
    var issueStore = new DefaultDataImportIssueStore();
    var nonTaxiTrip = TransitRepositoryForTest.trip("bus-route")
      .withRoute(TransitRepositoryForTest.route("bus-route").withMode(TransitMode.BUS).build())
      .withServiceId(TRIP.getServiceId())
      .build();
    var stopTimes = List.of(
      fullDayAreaStop(AREA_1, PickDrop.CALL_AGENCY, PickDrop.NONE, nonTaxiTrip),
      fullDayAreaStop(AREA_1, PickDrop.NONE, PickDrop.CALL_AGENCY, nonTaxiTrip)
    );
    var tripId = FeedScopedIdForTestFactory.id("t-bus");
    var trip = UnscheduledTrip.of(tripId).withTrip(nonTaxiTrip).withStopTimes(stopTimes).build();

    var zones = new TaxiZoneBuilder(issueStore).buildZones(List.of(trip));

    assertThat(zones).isEmpty();
    assertSingleTaxiZoneTripSkippedIssue(issueStore, tripId, "route mode is BUS");
  }

  @Test
  void boundedTimeRestrictionIsSkipped() {
    var issueStore = new DefaultDataImportIssueStore();
    var stopTimes = List.of(
      restrictedAreaStop(AREA_1, PickDrop.CALL_AGENCY, PickDrop.NONE),
      restrictedAreaStop(AREA_1, PickDrop.NONE, PickDrop.COORDINATE_WITH_DRIVER)
    );
    var trip = unscheduledTrip(stopTimes);

    var zones = new TaxiZoneBuilder(issueStore).buildZones(List.of(trip));

    assertThat(zones).isEmpty();
    assertSingleTaxiZoneTripSkippedIssue(issueStore, trip.getId(), "has a time restriction");
  }

  @Test
  void fullDayWindowIsAllowed() {
    var issueStore = new DefaultDataImportIssueStore();
    var trip = unscheduledTrip(validStopTimes());

    var zones = new TaxiZoneBuilder(issueStore).buildZones(List.of(trip));

    assertThat(zones).hasSize(1);
    assertThat(issueStore.listIssues()).isEmpty();
  }

  @Test
  void wrongNumberOfStopsIsSkipped() {
    var issueStore = new DefaultDataImportIssueStore();
    var stopTimes = List.of(
      fullDayAreaStop(AREA_1, PickDrop.CALL_AGENCY, PickDrop.NONE, TRIP),
      fullDayAreaStop(AREA_1, PickDrop.NONE, PickDrop.NONE, TRIP),
      fullDayAreaStop(AREA_1, PickDrop.NONE, PickDrop.CALL_AGENCY, TRIP)
    );
    var trip = unscheduledTrip(stopTimes);

    var zones = new TaxiZoneBuilder(issueStore).buildZones(List.of(trip));

    assertThat(zones).isEmpty();
    assertSingleTaxiZoneTripSkippedIssue(issueStore, trip.getId(), "expected exactly 2 stop times");
  }

  @Test
  void differentAreasIsSkipped() {
    var issueStore = new DefaultDataImportIssueStore();
    var stopTimes = List.of(
      fullDayAreaStop(AREA_1, PickDrop.CALL_AGENCY, PickDrop.NONE, TRIP),
      fullDayAreaStop(AREA_2, PickDrop.NONE, PickDrop.CALL_AGENCY, TRIP)
    );
    var trip = unscheduledTrip(stopTimes);

    var zones = new TaxiZoneBuilder(issueStore).buildZones(List.of(trip));

    assertThat(zones).isEmpty();
    assertSingleTaxiZoneTripSkippedIssue(
      issueStore,
      trip.getId(),
      "must reference the same GTFS Flex area"
    );
  }

  @ParameterizedTest
  @EnumSource(value = PickDrop.class, names = { "NONE", "COORDINATE_WITH_DRIVER" })
  void invalidPickupTypeIsSkipped(PickDrop pickupType) {
    var issueStore = new DefaultDataImportIssueStore();
    var stopTimes = List.of(
      fullDayAreaStop(AREA_1, pickupType, PickDrop.NONE, TRIP),
      fullDayAreaStop(AREA_1, PickDrop.NONE, PickDrop.CALL_AGENCY, TRIP)
    );
    var trip = unscheduledTrip(stopTimes);

    var zones = new TaxiZoneBuilder(issueStore).buildZones(List.of(trip));

    assertThat(zones).isEmpty();
    assertSingleTaxiZoneTripSkippedIssue(issueStore, trip.getId(), "stop 0 has pickup_type");
  }

  @ParameterizedTest
  @EnumSource(value = PickDrop.class, names = { "NONE", "COORDINATE_WITH_DRIVER" })
  void invalidDropOffTypeIsSkipped(PickDrop dropOffType) {
    var issueStore = new DefaultDataImportIssueStore();
    var stopTimes = List.of(
      fullDayAreaStop(AREA_1, PickDrop.CALL_AGENCY, PickDrop.NONE, TRIP),
      fullDayAreaStop(AREA_1, PickDrop.NONE, dropOffType, TRIP)
    );
    var trip = unscheduledTrip(stopTimes);

    var zones = new TaxiZoneBuilder(issueStore).buildZones(List.of(trip));

    assertThat(zones).isEmpty();
    assertSingleTaxiZoneTripSkippedIssue(issueStore, trip.getId(), "stop 1 has drop_off_type");
  }

  private static void assertSingleTaxiZoneTripSkippedIssue(
    DefaultDataImportIssueStore issueStore,
    FeedScopedId tripId,
    String expectedReasonFragment
  ) {
    var issues = issueStore.listIssues();
    assertThat(issues).hasSize(1);
    var issue = issues.get(0);
    assertThat(issue).isInstanceOf(TaxiZoneTripSkipped.class);
    var skipped = (TaxiZoneTripSkipped) issue;
    assertThat(skipped.tripId()).isEqualTo(tripId);
    assertThat(skipped.getMessage()).contains(expectedReasonFragment);
  }

  private static StopTime fullDayAreaStop(
    AreaStop areaStop,
    PickDrop pickupType,
    PickDrop dropOffType,
    Trip trip
  ) {
    return FlexStopTimesFactory.area(areaStop, "00:00", "24:00", trip, pickupType, dropOffType);
  }

  private static StopTime restrictedAreaStop(
    AreaStop areaStop,
    PickDrop pickupType,
    PickDrop dropOffType
  ) {
    return FlexStopTimesFactory.area(areaStop, "10:00", "10:30", TRIP, pickupType, dropOffType);
  }

  private static List<StopTime> validStopTimes() {
    return List.of(
      fullDayAreaStop(AREA_1, PickDrop.CALL_AGENCY, PickDrop.NONE, TRIP),
      fullDayAreaStop(AREA_1, PickDrop.NONE, PickDrop.CALL_AGENCY, TRIP)
    );
  }

  private static FlexTrip<?, ?> unscheduledTrip(List<StopTime> stopTimes) {
    return UnscheduledTrip.of(FeedScopedIdForTestFactory.id("t1"))
      .withTrip(TRIP)
      .withStopTimes(stopTimes)
      .build();
  }
}
