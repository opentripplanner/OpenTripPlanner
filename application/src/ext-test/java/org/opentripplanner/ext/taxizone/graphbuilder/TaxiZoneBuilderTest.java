package org.opentripplanner.ext.taxizone.graphbuilder;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.opentripplanner.core.model.id.FeedScopedIdForTestFactory;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.ext.flex.trip.ScheduledDeviatedTrip;
import org.opentripplanner.ext.flex.trip.UnscheduledTrip;
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
    var trip = unscheduledTrip(validStopTimes());
    var zones = TaxiZoneBuilder.buildZones(List.of(trip));

    assertThat(zones).hasSize(1);
    var zone = zones.get(0);
    assertThat(zone.geometry()).isEqualTo(AREA_1.getGeometry());
    assertThat(zone.route()).isEqualTo(TRIP.getRoute());
  }

  @Test
  void scheduledTripIsSkipped() {
    var stopTimes = List.of(
      FlexStopTimesFactory.area(AREA_1, "10:10", "10:15"),
      FlexStopTimesFactory.regularStop("10:40", "10:45")
    );
    var trip = ScheduledDeviatedTrip.of(FeedScopedIdForTestFactory.id("t2"))
      .withStopTimes(stopTimes)
      .build();

    var zones = TaxiZoneBuilder.buildZones(List.of(trip));

    assertThat(zones).isEmpty();
  }

  @Test
  void nonTaxiRouteTypeIsSkipped() {
    var nonTaxiTrip = TransitRepositoryForTest.trip("bus-route")
      .withRoute(TransitRepositoryForTest.route("bus-route").withMode(TransitMode.BUS).build())
      .withServiceId(TRIP.getServiceId())
      .build();
    var stopTimes = List.of(
      fullDayAreaStop(AREA_1, PickDrop.CALL_AGENCY, PickDrop.NONE, nonTaxiTrip),
      fullDayAreaStop(AREA_1, PickDrop.NONE, PickDrop.CALL_AGENCY, nonTaxiTrip)
    );
    var trip = UnscheduledTrip.of(FeedScopedIdForTestFactory.id("t-bus"))
      .withTrip(nonTaxiTrip)
      .withStopTimes(stopTimes)
      .build();

    var zones = TaxiZoneBuilder.buildZones(List.of(trip));

    assertThat(zones).isEmpty();
  }

  @Test
  void boundedTimeRestrictionIsSkipped() {
    var stopTimes = List.of(
      restrictedAreaStop(AREA_1, PickDrop.CALL_AGENCY, PickDrop.NONE),
      restrictedAreaStop(AREA_1, PickDrop.NONE, PickDrop.COORDINATE_WITH_DRIVER)
    );
    var trip = unscheduledTrip(stopTimes);

    var zones = TaxiZoneBuilder.buildZones(List.of(trip));

    assertThat(zones).isEmpty();
  }

  @Test
  void fullDayWindowIsAllowed() {
    var trip = unscheduledTrip(validStopTimes());

    var zones = TaxiZoneBuilder.buildZones(List.of(trip));

    assertThat(zones).hasSize(1);
  }

  @Test
  void wrongNumberOfStopsIsSkipped() {
    var stopTimes = List.of(
      fullDayAreaStop(AREA_1, PickDrop.CALL_AGENCY, PickDrop.NONE, TRIP),
      fullDayAreaStop(AREA_1, PickDrop.NONE, PickDrop.NONE, TRIP),
      fullDayAreaStop(AREA_1, PickDrop.NONE, PickDrop.CALL_AGENCY, TRIP)
    );
    var trip = unscheduledTrip(stopTimes);

    var zones = TaxiZoneBuilder.buildZones(List.of(trip));

    assertThat(zones).isEmpty();
  }

  @Test
  void differentAreasIsSkipped() {
    var stopTimes = List.of(
      fullDayAreaStop(AREA_1, PickDrop.CALL_AGENCY, PickDrop.NONE, TRIP),
      fullDayAreaStop(AREA_2, PickDrop.NONE, PickDrop.CALL_AGENCY, TRIP)
    );
    var trip = unscheduledTrip(stopTimes);

    var zones = TaxiZoneBuilder.buildZones(List.of(trip));

    assertThat(zones).isEmpty();
  }

  @ParameterizedTest
  @EnumSource(value = PickDrop.class, names = { "NONE", "COORDINATE_WITH_DRIVER" })
  void invalidPickupTypeIsSkipped(PickDrop pickupType) {
    var stopTimes = List.of(
      fullDayAreaStop(AREA_1, pickupType, PickDrop.NONE, TRIP),
      fullDayAreaStop(AREA_1, PickDrop.NONE, PickDrop.CALL_AGENCY, TRIP)
    );
    var trip = unscheduledTrip(stopTimes);

    var zones = TaxiZoneBuilder.buildZones(List.of(trip));

    assertThat(zones).isEmpty();
  }

  @ParameterizedTest
  @EnumSource(value = PickDrop.class, names = { "NONE", "COORDINATE_WITH_DRIVER" })
  void invalidDropOffTypeIsSkipped(PickDrop dropOffType) {
    var stopTimes = List.of(
      fullDayAreaStop(AREA_1, PickDrop.CALL_AGENCY, PickDrop.NONE, TRIP),
      fullDayAreaStop(AREA_1, PickDrop.NONE, dropOffType, TRIP)
    );
    var trip = unscheduledTrip(stopTimes);

    var zones = TaxiZoneBuilder.buildZones(List.of(trip));

    assertThat(zones).isEmpty();
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
