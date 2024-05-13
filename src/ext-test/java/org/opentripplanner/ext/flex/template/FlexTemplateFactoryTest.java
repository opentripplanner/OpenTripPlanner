package org.opentripplanner.ext.flex.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.ext.flex.template.FlexTemplateFactoryTest.BoardAlight.ALIGHT_ONLY;
import static org.opentripplanner.ext.flex.template.FlexTemplateFactoryTest.BoardAlight.BOARD_AND_ALIGHT;
import static org.opentripplanner.ext.flex.template.FlexTemplateFactoryTest.BoardAlight.BOARD_ONLY;
import static org.opentripplanner.framework.time.TimeUtils.time;

import gnu.trove.set.hash.TIntHashSet;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.ext.flex.FlexServiceDate;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.ext.flex.flexpathcalculator.ScheduledFlexPathCalculator;
import org.opentripplanner.ext.flex.flexpathcalculator.StreetFlexPathCalculator;
import org.opentripplanner.ext.flex.trip.ScheduledDeviatedTrip;
import org.opentripplanner.ext.flex.trip.UnscheduledTrip;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.street.model.vertex.StreetLocation;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;

class FlexTemplateFactoryTest {

  private static final TransitModelForTest MODEL = TransitModelForTest.of();

  /**
   * This is pass-through information
   */
  private static final Duration MAX_TRANSFER_DURATION = Duration.ofMinutes(10);

  /**
   * Any calculator will do. The only thing we will test here is that a new scheduled calculator
   * is created for scheduled-flex-trips.
   */
  private static final FlexPathCalculator CALCULATOR = new StreetFlexPathCalculator(
    false,
    Duration.ofHours(3)
  );

  public static final int SERVICE_TIME_OFFSET = 3600 * 2;

  /**
   * The date is pass-through information in this test, so one date is enough.
   */
  private static final FlexServiceDate DATE = new FlexServiceDate(
    LocalDate.of(2024, Month.MAY, 17),
    SERVICE_TIME_OFFSET,
    new TIntHashSet()
  );

  // Stop A-D is a mix of regular and area stops - it should not matter for this test
  private static final StopLocation STOP_A = MODEL.stop("A").build();
  private static final StopLocation STOP_B = MODEL.areaStop("B").build();
  private static final StopLocation STOP_C = MODEL.areaStop("C").build();
  private static final StopLocation STOP_D = MODEL.stop("D").build();
  private static final RegularStop STOP_G1 = MODEL.stop("G1").build();
  private static final RegularStop STOP_G2 = MODEL.stop("G2").build();
  private static final RegularStop STOP_G3 = MODEL.stop("G3").build();
  private static final RegularStop STOP_G4 = MODEL.stop("G4").build();
  private static final StopLocation GROUP_STOP_12 = MODEL.groupStop("G", STOP_G1, STOP_G2);
  private static final StopLocation GROUP_STOP_34 = MODEL.groupStop("G", STOP_G3, STOP_G4);

  private static final Trip TRIP = TransitModelForTest.trip("Trip").build();
  private static final int T_10_00 = time("10:00");
  private static final int T_10_10 = time("10:10");
  private static final int T_10_20 = time("10:20");
  private static final int T_10_30 = time("10:30");
  private static final int T_10_40 = time("10:40");

  @Test
  void testCreateAccessTemplateForUnscheduledTripWithTwoStopsAndNoBoardRestrictions() {
    var flexTrip = unscheduledTrip(
      "FlexTrip",
      stopTime(1, STOP_A, BOARD_AND_ALIGHT, T_10_00),
      stopTime(2, STOP_B, BOARD_AND_ALIGHT, T_10_10)
    );

    var factory = FlexTemplateFactory.of(CALCULATOR, MAX_TRANSFER_DURATION);

    // Create template with access boarding at stop A
    var subject = factory.with(DATE, flexTrip, nearbyStop(STOP_A)).createAccessTemplates();

    var template = subject.get(0);
    assertEquals(0, template.fromStopIndex);
    assertEquals(1, template.toStopIndex);
    assertSame(CALCULATOR, template.calculator);
    assertSame(STOP_B, template.transferStop);
    assertSame(DATE.serviceDate, template.serviceDate);
    assertEquals(SERVICE_TIME_OFFSET, template.secondsFromStartOfTime);
    assertEquals(1, subject.size(), subject::toString);

    // We are not allowed to board and alight at the same stop so boarding the last stop
    // will result in an empty result
    subject = factory.with(DATE, flexTrip, nearbyStop(STOP_B)).createAccessTemplates();
    assertTrue(subject.isEmpty(), subject::toString);

    // Search for a stop not part of the pattern should result in an empty result
    subject = factory.with(DATE, flexTrip, nearbyStop(STOP_C)).createAccessTemplates();
    assertTrue(subject.isEmpty(), subject::toString);
  }

  @Test
  void testCreateEgressTemplateForUnscheduledTripWithTwoStopsAndNoBoardRestrictions() {
    var flexTrip = unscheduledTrip(
      "FlexTrip",
      stopTime(1, STOP_A, BOARD_AND_ALIGHT, T_10_00),
      stopTime(2, STOP_B, BOARD_AND_ALIGHT, T_10_10)
    );

    var factory = FlexTemplateFactory.of(CALCULATOR, MAX_TRANSFER_DURATION);

    // Create template with egress alighting at stop B
    var subject = factory.with(DATE, flexTrip, nearbyStop(STOP_B)).createEgressTemplates();

    var template = subject.get(0);
    assertEquals(0, template.fromStopIndex);
    assertEquals(1, template.toStopIndex);
    assertSame(CALCULATOR, template.calculator);
    assertSame(STOP_A, template.transferStop);
    assertSame(DATE.serviceDate, template.serviceDate);
    assertEquals(SERVICE_TIME_OFFSET, template.secondsFromStartOfTime);
    assertEquals(1, subject.size(), subject::toString);

    // We are not allowed to board and alight at the same stop so boarding the last stop
    // will result in an empty result
    subject = factory.with(DATE, flexTrip, nearbyStop(STOP_A)).createEgressTemplates();
    assertTrue(subject.isEmpty(), subject::toString);

    // Search for a stop not part of the pattern should result in an empty result
    subject = factory.with(DATE, flexTrip, nearbyStop(STOP_C)).createEgressTemplates();
    assertTrue(subject.isEmpty(), subject::toString);
  }

  @Test
  void testCreateAccessTemplateForUnscheduledTripWithBoardAndAlightRestrictions() {
    var flexTrip = unscheduledTrip(
      "FlexTrip",
      stopTime(1, STOP_A, BOARD_ONLY, T_10_00),
      stopTime(2, STOP_B, ALIGHT_ONLY, T_10_10),
      stopTime(3, STOP_C, BOARD_ONLY, T_10_20),
      stopTime(4, STOP_D, ALIGHT_ONLY, T_10_30)
    );

    var factory = FlexTemplateFactory.of(CALCULATOR, MAX_TRANSFER_DURATION);

    // Create template with boarding at stop A
    var subject = factory.with(DATE, flexTrip, nearbyStop(STOP_A)).createAccessTemplates();

    var t1 = subject.get(0);
    var t2 = subject.get(1);

    assertEquals(0, t1.fromStopIndex);
    assertEquals(0, t2.fromStopIndex);
    assertEquals(Set.of(1, 3), Set.of(t1.toStopIndex, t2.toStopIndex));
    assertEquals(2, subject.size());

    // Board at stop C
    subject = factory.with(DATE, flexTrip, nearbyStop(STOP_C)).createAccessTemplates();

    t1 = subject.get(0);
    assertEquals(2, t1.fromStopIndex);
    assertEquals(3, t1.toStopIndex);
    assertEquals(1, subject.size());

    // We are not allowed to board and alight at the same stop so boarding the last stop
    // will result in an empty result
    subject = factory.with(DATE, flexTrip, nearbyStop(STOP_B)).createAccessTemplates();
    assertTrue(subject.isEmpty(), subject::toString);

    // Search for a stop not part of the pattern should result in an empty result
    subject = factory.with(DATE, flexTrip, nearbyStop(STOP_D)).createAccessTemplates();
    assertTrue(subject.isEmpty(), subject::toString);
  }

  @Test
  void testCreateEgressTemplateForUnscheduledTripWithBoardAndAlightRestrictions() {
    var flexTrip = unscheduledTrip(
      "FlexTrip",
      stopTime(1, STOP_A, BOARD_ONLY, T_10_00),
      stopTime(2, STOP_B, ALIGHT_ONLY, T_10_10),
      stopTime(3, STOP_C, BOARD_ONLY, T_10_20),
      stopTime(4, STOP_D, ALIGHT_ONLY, T_10_30)
    );

    var factory = FlexTemplateFactory.of(CALCULATOR, MAX_TRANSFER_DURATION);

    // Create template with boarding at stop A
    var subject = factory.with(DATE, flexTrip, nearbyStop(STOP_D)).createEgressTemplates();

    var t1 = subject.get(0);
    var t2 = subject.get(1);

    assertEquals(Set.of(0, 2), Set.of(t1.fromStopIndex, t2.fromStopIndex));
    assertEquals(3, t1.toStopIndex);
    assertEquals(3, t2.toStopIndex);
    assertEquals(2, subject.size());

    // Board at stop C
    subject = factory.with(DATE, flexTrip, nearbyStop(STOP_B)).createEgressTemplates();

    t1 = subject.get(0);
    assertEquals(0, t1.fromStopIndex);
    assertEquals(1, t1.toStopIndex);
    assertEquals(1, subject.size());

    // We are not allowed to board and alight at the same stop so boarding the last stop
    // will result in an empty result
    subject = factory.with(DATE, flexTrip, nearbyStop(STOP_C)).createEgressTemplates();
    assertTrue(subject.isEmpty(), subject::toString);

    // Search for a stop not part of the pattern should result in an empty result
    subject = factory.with(DATE, flexTrip, nearbyStop(STOP_A)).createEgressTemplates();
    assertTrue(subject.isEmpty(), subject::toString);
  }

  @Test
  void testCreateAccessTemplateForUnscheduledTripWithTwoGroupsStops() {
    var flexTrip = unscheduledTrip(
      "FlexTrip",
      stopTime(1, GROUP_STOP_12, BOARD_ONLY, T_10_00),
      stopTime(2, GROUP_STOP_34, ALIGHT_ONLY, T_10_20)
    );

    var factory = FlexTemplateFactory.of(CALCULATOR, MAX_TRANSFER_DURATION);

    // Create template with access boarding at stop A
    var subject = factory.with(DATE, flexTrip, nearbyStop(STOP_G1)).createAccessTemplates();

    var t1 = subject.get(0);
    var t2 = subject.get(1);
    assertEquals(0, t1.fromStopIndex);
    assertEquals(0, t2.fromStopIndex);
    assertEquals(1, t1.toStopIndex);
    assertEquals(1, t2.toStopIndex);
    assertEquals(Set.of(STOP_G3, STOP_G4), Set.of(t1.transferStop, t2.transferStop));
    assertEquals(2, subject.size(), subject::toString);
  }

  @Test
  void testCreateEgressTemplateForUnscheduledTripWithTwoGroupsStops() {
    var flexTrip = unscheduledTrip(
      "FlexTrip",
      stopTime(1, GROUP_STOP_12, BOARD_ONLY, T_10_00),
      stopTime(2, GROUP_STOP_34, ALIGHT_ONLY, T_10_20)
    );

    var factory = FlexTemplateFactory.of(CALCULATOR, MAX_TRANSFER_DURATION);

    // Create template with access boarding at stop A
    var subject = factory.with(DATE, flexTrip, nearbyStop(STOP_G4)).createEgressTemplates();

    var t1 = subject.get(0);
    var t2 = subject.get(1);
    assertEquals(0, t1.fromStopIndex);
    assertEquals(0, t2.fromStopIndex);
    assertEquals(1, t1.toStopIndex);
    assertEquals(1, t2.toStopIndex);
    assertEquals(Set.of(STOP_G1, STOP_G2), Set.of(t1.transferStop, t2.transferStop));
    assertEquals(2, subject.size(), subject::toString);
  }

  @Test
  void testCreateAccessTemplateForScheduledDeviatedTrip() {
    var flexTrip = scheduledDeviatedFlexTrip(
      "FlexTrip",
      stopTime(1, STOP_A, BOARD_ONLY, T_10_00),
      stopTime(5, STOP_B, BOARD_AND_ALIGHT, T_10_20),
      stopTime(10, STOP_C, ALIGHT_ONLY, T_10_30)
    );

    var factory = FlexTemplateFactory.of(CALCULATOR, MAX_TRANSFER_DURATION);

    // Create template with access boarding at stop A
    var subject = factory.with(DATE, flexTrip, nearbyStop(STOP_B)).createAccessTemplates();

    var template = subject.get(0);
    assertEquals(1, template.fromStopIndex);
    assertEquals(2, template.toStopIndex);
    assertEquals(STOP_C, template.transferStop);
    assertTrue(template.calculator instanceof ScheduledFlexPathCalculator);
    assertEquals(1, subject.size(), subject::toString);
  }

  @Test
  void testCreateEgressTemplateForScheduledDeviatedTrip() {
    var flexTrip = scheduledDeviatedFlexTrip(
      "FlexTrip",
      stopTime(1, STOP_A, BOARD_ONLY, T_10_00),
      stopTime(5, STOP_B, BOARD_AND_ALIGHT, T_10_20),
      stopTime(10, STOP_C, ALIGHT_ONLY, T_10_30)
    );

    var factory = FlexTemplateFactory.of(CALCULATOR, MAX_TRANSFER_DURATION);

    // Create template with access boarding at stop A
    var subject = factory.with(DATE, flexTrip, nearbyStop(STOP_B)).createEgressTemplates();

    var template = subject.get(0);
    assertEquals(0, template.fromStopIndex);
    assertEquals(1, template.toStopIndex);
    assertEquals(STOP_A, template.transferStop);
    assertTrue(template.calculator instanceof ScheduledFlexPathCalculator);
    assertEquals(1, subject.size(), subject::toString);
  }

  /**
   * The nearbyStop is pass-through information, except the stop - which defines the "transfer"
   * point.
   */
  private static NearbyStop nearbyStop(StopLocation transferPoint) {
    var id = "NearbyStop:" + transferPoint.getId().getId();
    return new NearbyStop(
      transferPoint,
      0,
      List.of(),
      new State(
        new StreetLocation(id, new Coordinate(0, 0), I18NString.of(id)),
        StreetSearchRequest.of().build()
      )
    );
  }

  private static ScheduledDeviatedTrip scheduledDeviatedFlexTrip(String id, StopTime... stopTimes) {
    return MODEL.scheduledDeviatedTrip(id, stopTimes);
  }

  private static UnscheduledTrip unscheduledTrip(String id, StopTime... stopTimes) {
    return MODEL.unscheduledTrip(id, Arrays.asList(stopTimes));
  }

  private static StopTime stopTime(
    int seqNr,
    StopLocation stop,
    BoardAlight boardAlight,
    int startTime
  ) {
    var st = MODEL.stopTime(TRIP, seqNr, stop);
    switch (boardAlight) {
      case BOARD_ONLY:
        st.setDropOffType(PickDrop.NONE);
        break;
      case ALIGHT_ONLY:
        st.setPickupType(PickDrop.NONE);
        break;
    }
    st.setFlexWindowStart(startTime);
    // 5-minute window
    st.setFlexWindowEnd(startTime + 300);
    return st;
  }

  enum BoardAlight {
    BOARD_ONLY,
    ALIGHT_ONLY,
    BOARD_AND_ALIGHT,
  }
}
