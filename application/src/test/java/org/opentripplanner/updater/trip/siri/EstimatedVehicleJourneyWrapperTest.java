package org.opentripplanner.updater.trip.siri;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertFailure;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.LocalTimeParser;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.timetable.OccupancyStatus;
import org.opentripplanner.updater.spi.UpdateErrorType;
import uk.org.siri.siri21.OccupancyEnumeration;
import uk.org.siri.siri21.VehicleModesEnumeration;

class EstimatedVehicleJourneyWrapperTest {

  private static final LocalTimeParser TIME_PARSER = new LocalTimeParser(
    ZoneId.of("Europe/Paris"),
    LocalDate.of(2024, 5, 7)
  );

  /* Construction and validation */

  @Test
  void rejectUnmonitoredJourney() {
    var journey = builder().withMonitored(false).buildEstimatedVehicleJourney();

    assertFailure(UpdateErrorType.NOT_MONITORED, () -> EstimatedVehicleJourneyWrapper.of(journey));
  }

  @Test
  void acceptUnmonitoredCancellation() {
    var journey = builder()
      .withMonitored(false)
      .withCancellation(true)
      .buildEstimatedVehicleJourney();

    var wrapper = EstimatedVehicleJourneyWrapper.of(journey);

    assertFalse(wrapper.isMonitored());
    assertTrue(wrapper.isCancellation());
  }

  @Test
  void propagateInvalidCallFailure() {
    var journey = builder()
      .withEstimatedCalls(calls -> calls.call("STOP_A").clearOrder())
      .buildEstimatedVehicleJourney();

    assertFailure(UpdateErrorType.MISSING_CALL_ORDER, () ->
      EstimatedVehicleJourneyWrapper.of(journey)
    );
  }

  /* Calls */

  @Test
  void calls() {
    var journey = builder()
      .withEstimatedCalls(calls -> calls.call("STOP_A").call("STOP_B"))
      .buildEstimatedVehicleJourney();

    var wrapper = EstimatedVehicleJourneyWrapper.of(journey);

    assertEquals(
      List.of("STOP_A", "STOP_B"),
      wrapper.calls().stream().map(CallWrapper::getStopPointRef).toList()
    );
  }

  @Test
  void hasExtraCall() {
    var withExtraCall = builder()
      .withEstimatedCalls(calls -> calls.call("STOP_A").call("STOP_B").withIsExtraCall(true))
      .buildEstimatedVehicleJourney();
    assertTrue(EstimatedVehicleJourneyWrapper.of(withExtraCall).hasExtraCall());

    var withoutExtraCall = builder()
      .withEstimatedCalls(calls -> calls.call("STOP_A").call("STOP_B"))
      .buildEstimatedVehicleJourney();
    assertFalse(EstimatedVehicleJourneyWrapper.of(withoutExtraCall).hasExtraCall());
  }

  /* Journey status */

  @Test
  void journeyStatusFlags() {
    var journey = builder()
      .withCancellation(true)
      .withIsExtraJourney(true)
      .withPredictionInaccurate(true)
      .buildEstimatedVehicleJourney();

    var wrapper = EstimatedVehicleJourneyWrapper.of(journey);

    assertTrue(wrapper.isMonitored());
    assertTrue(wrapper.isCancellation());
    assertTrue(wrapper.isExtraJourney());
    assertTrue(wrapper.isPredictionInaccurate());
  }

  @Test
  void journeyStatusFlagsDefaultToFalse() {
    var wrapper = EstimatedVehicleJourneyWrapper.of(builder().buildEstimatedVehicleJourney());

    assertFalse(wrapper.isCancellation());
    assertFalse(wrapper.isExtraJourney());
    assertFalse(wrapper.isPredictionInaccurate());
  }

  /* Trip identification */

  @Test
  void datedVehicleJourneyRef() {
    var journey = builder().withDatedVehicleJourneyRef("DSJ:1").buildEstimatedVehicleJourney();

    assertEquals("DSJ:1", EstimatedVehicleJourneyWrapper.of(journey).datedVehicleJourneyRef());
  }

  @Test
  void code() {
    var journey = builder()
      .withEstimatedVehicleJourneyCode("RUT:ServiceJourney:1234")
      .buildEstimatedVehicleJourney();

    var code = EstimatedVehicleJourneyWrapper.of(journey).code();

    // The EstimatedVehicleJourneyCode can be viewed as either entity type.
    assertEquals("RUT:ServiceJourney:1234", code.asServiceJourneyId());
    assertEquals("RUT:DatedServiceJourney:1234", code.asDatedServiceJourneyId());
  }

  @Test
  void vehicleJourneyIdAndServiceDate() {
    var journey = builder()
      .withFramedVehicleJourneyRef(ref ->
        ref.withVehicleJourneyRef("SJ:1").withServiceDate(LocalDate.of(2024, 5, 7))
      )
      .buildEstimatedVehicleJourney();

    var result = EstimatedVehicleJourneyWrapper.of(journey).vehicleJourneyIdAndServiceDate();

    assertEquals("SJ:1", result.vehicleJourneyId());
    assertEquals("2024-05-07", result.serviceDate());
  }

  @Test
  void internalPlanningCode() {
    var journey = builder().withVehicleRef("VEHICLE:1").buildEstimatedVehicleJourney();

    assertEquals("VEHICLE:1", EstimatedVehicleJourneyWrapper.of(journey).internalPlanningCode());
  }

  /* Replaced trips */

  @Test
  void replacedDatedVehicleJourneyRef() {
    var journey = builder().withVehicleJourneyRef("REPLACED:1").buildEstimatedVehicleJourney();

    assertEquals(
      "REPLACED:1",
      EstimatedVehicleJourneyWrapper.of(journey).replacedDatedVehicleJourneyRef()
    );
  }

  @Test
  void additionalReplacedDatedVehicleJourneyRefs() {
    var journey = builder().buildEstimatedVehicleJourney();
    journey
      .getAdditionalVehicleJourneyReves()
      .add(
        new SiriEtBuilder.FramedVehicleRefBuilder()
          .withVehicleJourneyRef("REPLACED:2")
          .withServiceDate(LocalDate.of(2024, 5, 7))
          .build()
      );

    var result = EstimatedVehicleJourneyWrapper.of(
      journey
    ).additionalReplacedDatedVehicleJourneyRefs();

    assertEquals(1, result.size());
    assertEquals("REPLACED:2", result.getFirst().vehicleJourneyId());
    assertEquals("2024-05-07", result.getFirst().serviceDate());
  }

  @Test
  void externalLineRef() {
    var journey = builder().withExternalLineRef("LINE:ext").buildEstimatedVehicleJourney();

    assertEquals("LINE:ext", EstimatedVehicleJourneyWrapper.of(journey).externalLineRef());
  }

  /* Line, operator and mode */

  @Test
  void lineAndOperatorRef() {
    var journey = builder()
      .withLineRef("LINE:1")
      .withOperatorRef("OPERATOR:1")
      .buildEstimatedVehicleJourney();

    var wrapper = EstimatedVehicleJourneyWrapper.of(journey);

    assertEquals("LINE:1", wrapper.lineRef());
    assertEquals("OPERATOR:1", wrapper.operatorRef());
  }

  @Test
  void vehicleModes() {
    var rail = builder()
      .withVehicleMode(VehicleModesEnumeration.RAIL)
      .buildEstimatedVehicleJourney();
    var railWrapper = EstimatedVehicleJourneyWrapper.of(rail);
    assertTrue(railWrapper.isRail());
    assertEquals(TransitMode.RAIL, railWrapper.transitMode());

    var bus = builder().withVehicleMode(VehicleModesEnumeration.BUS).buildEstimatedVehicleJourney();
    var busWrapper = EstimatedVehicleJourneyWrapper.of(bus);
    assertFalse(busWrapper.isRail());
    assertEquals(TransitMode.BUS, busWrapper.transitMode());
  }

  /* Descriptive information */

  @Test
  void descriptiveInformation() {
    var journey = builder()
      .withPublishedLineName("Line 1")
      .withDestinationName("Central Station")
      .withOccupancy(OccupancyEnumeration.FULL)
      .buildEstimatedVehicleJourney();

    var wrapper = EstimatedVehicleJourneyWrapper.of(journey);

    assertEquals("Line 1", wrapper.publishedLineName());
    assertEquals("Central Station", wrapper.destinationName());
    assertEquals(OccupancyStatus.FULL, wrapper.occupancy());
    assertEquals("DATASOURCE", wrapper.dataSource());
  }

  /* Null-safety of optional references */

  @Test
  void accessorsAreNullSafeOnMinimalJourney() {
    var wrapper = EstimatedVehicleJourneyWrapper.of(builder().buildEstimatedVehicleJourney());

    assertNull(wrapper.lineRef());
    assertNull(wrapper.operatorRef());
    assertNull(wrapper.datedVehicleJourneyRef());
    assertNull(wrapper.code());
    assertNull(wrapper.vehicleJourneyIdAndServiceDate());
    assertNull(wrapper.internalPlanningCode());
    assertNull(wrapper.replacedDatedVehicleJourneyRef());
    assertNull(wrapper.externalLineRef());
    assertNull(wrapper.occupancy());
    // Natural-language accessors default to an empty string rather than null.
    assertEquals("", wrapper.publishedLineName());
    assertEquals("", wrapper.destinationName());
    assertTrue(wrapper.calls().isEmpty());
    assertFalse(wrapper.hasExtraCall());
    assertTrue(wrapper.additionalReplacedDatedVehicleJourneyRefs().isEmpty());
    assertEquals(TransitMode.BUS, wrapper.transitMode());
    assertFalse(wrapper.isRail());
  }

  private static SiriEtBuilder builder() {
    return new SiriEtBuilder(TIME_PARSER);
  }
}
