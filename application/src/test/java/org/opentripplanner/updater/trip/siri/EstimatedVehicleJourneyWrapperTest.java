package org.opentripplanner.updater.trip.siri;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertFailure;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
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

    Optional<String> datedVehicleJourneyRef = EstimatedVehicleJourneyWrapper.of(
      journey
    ).datedVehicleJourneyRef();
    assertThat(datedVehicleJourneyRef).hasValue("DSJ:1");
  }

  @Test
  void code() {
    var journey = builder()
      .withEstimatedVehicleJourneyCode("RUT:ServiceJourney:1234")
      .buildEstimatedVehicleJourney();

    var code = EstimatedVehicleJourneyWrapper.of(journey).code();

    // The EstimatedVehicleJourneyCode can be viewed as either entity type.
    assertTrue(code.isPresent());
    assertEquals("RUT:ServiceJourney:1234", code.get().asServiceJourneyId());
    assertEquals("RUT:DatedServiceJourney:1234", code.get().asDatedServiceJourneyId());
  }

  @Test
  void vehicleJourneyIdAndServiceDate() {
    var journey = builder()
      .withFramedVehicleJourneyRef(ref ->
        ref.withVehicleJourneyRef("SJ:1").withServiceDate(LocalDate.of(2024, 5, 7))
      )
      .buildEstimatedVehicleJourney();

    var result = EstimatedVehicleJourneyWrapper.of(journey).vehicleJourneyIdAndServiceDate();

    assertTrue(result.isPresent());
    assertEquals("SJ:1", result.get().vehicleJourneyId());
    assertEquals(LocalDate.of(2024, 5, 7), result.get().serviceDate());
  }

  @Test
  void vehicleRef() {
    var journey = builder().withVehicleRef("VEHICLE:1").buildEstimatedVehicleJourney();

    var vehicleRef = EstimatedVehicleJourneyWrapper.of(journey).vehicleRef();
    assertThat(vehicleRef).hasValue("VEHICLE:1");
  }

  @Test
  void vehicleRefIsEmptyWhenAbsent() {
    var journey = builder().buildEstimatedVehicleJourney();

    assertThat(EstimatedVehicleJourneyWrapper.of(journey).vehicleRef()).isEmpty();
  }

  /* Replaced trips */

  @Test
  void replacedDatedVehicleJourneyRef() {
    var journey = builder().withVehicleJourneyRef("REPLACED:1").buildEstimatedVehicleJourney();

    var replacedDatedVehicleJourneyRef = EstimatedVehicleJourneyWrapper.of(
      journey
    ).replacedDatedVehicleJourneyRef();
    assertThat(replacedDatedVehicleJourneyRef).hasValue("REPLACED:1");
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
    assertEquals(LocalDate.of(2024, 5, 7), result.getFirst().serviceDate());
  }

  @Test
  void externalLineRef() {
    var journey = builder().withExternalLineRef("LINE:ext").buildEstimatedVehicleJourney();

    var externalLineRef = EstimatedVehicleJourneyWrapper.of(journey).externalLineRef();
    assertThat(externalLineRef).hasValue("LINE:ext");
  }

  /* Line, operator and mode */

  @Test
  void lineAndOperatorRef() {
    var journey = builder()
      .withLineRef("LINE:1")
      .withOperatorRef("OPERATOR:1")
      .buildEstimatedVehicleJourney();

    var wrapper = EstimatedVehicleJourneyWrapper.of(journey);

    assertThat(wrapper.lineRef()).hasValue("LINE:1");
    assertThat(wrapper.operatorRef()).hasValue("OPERATOR:1");
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
    assertThat(wrapper.occupancy()).hasValue(OccupancyStatus.FULL);
    assertThat(wrapper.dataSource()).hasValue("DATASOURCE");
  }

  /* Null-safety of optional references */

  @Test
  void accessorsAreNullSafeOnMinimalJourney() {
    var wrapper = EstimatedVehicleJourneyWrapper.of(builder().buildEstimatedVehicleJourney());

    assertThat(wrapper.lineRef()).isEmpty();
    assertThat(wrapper.operatorRef()).isEmpty();
    assertThat(wrapper.datedVehicleJourneyRef()).isEmpty();
    assertThat(wrapper.code()).isEmpty();
    assertThat(wrapper.vehicleJourneyIdAndServiceDate()).isEmpty();
    assertThat(wrapper.vehicleRef()).isEmpty();
    assertThat(wrapper.replacedDatedVehicleJourneyRef()).isEmpty();
    assertThat(wrapper.externalLineRef()).isEmpty();
    assertThat(wrapper.occupancy()).isEmpty();
    // Natural-language accessors default to an empty string rather than empty.
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
