package org.opentripplanner.updater.elevator.siri;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.service.elevatorstatus.model.ElevatorStatus.inService;
import static org.opentripplanner.service.elevatorstatus.model.ElevatorStatus.outOfService;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.entur.siri21.util.SiriXml;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.opentripplanner.service.elevatorstatus.model.ElevatorStatus;
import uk.org.siri.siri21.Siri;

class SiriFmElevatorMapperTest {

  private static final String A = "Lift Maker:1";
  private static final String B = "Lift Maker:2";

  @ParameterizedTest
  @CsvSource({
    "available, IN_SERVICE, true",
    "added, IN_SERVICE, true",
    "notAvailable, IN_SERVICE, false",
    "removed, IN_SERVICE, false",
    "unknown, IN_SERVICE, true",
    "unknown, OUT_OF_SERVICE, false",
    "unknown, KEEP_PREVIOUS, true",
    "partiallyAvailable, IN_SERVICE, true",
    "partiallyAvailable, OUT_OF_SERVICE, false",
  })
  void mapStatus(String status, UnknownElevatorStatus unknownStatus, boolean expectedInService) {
    var mapper = new SiriFmElevatorMapper(unknownStatus);

    assertEquals(
      List.of(new ElevatorStatus(A, expectedInService)),
      mapper.map(siri(condition(A, status, "lift"))).orElseThrow()
    );
  }

  @Test
  void aMissingStatusIsUnknown() {
    var mapper = new SiriFmElevatorMapper(UnknownElevatorStatus.OUT_OF_SERVICE);

    assertEquals(
      List.of(outOfService(A)),
      mapper.map(siri(condition(A, null, "lift"))).orElseThrow()
    );
  }

  @Test
  void keepPreviousUsesTheLastKnownStatus() {
    var mapper = new SiriFmElevatorMapper(UnknownElevatorStatus.KEEP_PREVIOUS);

    mapper
      .map(siri(condition(A, "notAvailable", "lift"), condition(B, "available", "lift")))
      .orElseThrow();
    assertEquals(
      List.of(outOfService(A), inService(B)),
      mapper
        .map(siri(condition(A, "unknown", "lift"), condition(B, "unknown", "lift")))
        .orElseThrow()
    );
    // Still unknown: the last known status is kept
    assertEquals(
      List.of(outOfService(A)),
      mapper.map(siri(condition(A, "unknown", "lift"))).orElseThrow()
    );
  }

  @Test
  void keepPreviousForgetsAnElevatorThatIsNotInTheDelivery() {
    var mapper = new SiriFmElevatorMapper(UnknownElevatorStatus.KEEP_PREVIOUS);

    mapper.map(siri(condition(A, "notAvailable", "lift"))).orElseThrow();
    mapper.map(siri(condition(B, "available", "lift"))).orElseThrow();

    assertEquals(
      List.of(inService(A)),
      mapper.map(siri(condition(A, "unknown", "lift"))).orElseThrow()
    );
  }

  @Test
  void facilitiesThatAreNotLiftsAreSkipped() {
    var mapper = new SiriFmElevatorMapper(UnknownElevatorStatus.IN_SERVICE);

    assertEquals(
      List.of(outOfService(A), outOfService(B)),
      mapper
        .map(
          siri(
            condition(A, "notAvailable", "lift"),
            condition("Escalator:1", "notAvailable", "escalator"),
            condition(B, "notAvailable", null)
          )
        )
        .orElseThrow()
    );
  }

  @Test
  void anEmptyDeliveryHasNoElevators() {
    var mapper = new SiriFmElevatorMapper(UnknownElevatorStatus.IN_SERVICE);

    assertEquals(List.of(), mapper.map(siri()).orElseThrow());
  }

  @Test
  void aResponseWithoutAFacilityMonitoringDeliveryIsNotMapped() {
    var mapper = new SiriFmElevatorMapper(UnknownElevatorStatus.IN_SERVICE);

    assertEquals(Optional.empty(), mapper.map(serviceDelivery("")));
  }

  @Test
  void aDeliveryWithAnErrorConditionIsNotMapped() {
    var mapper = new SiriFmElevatorMapper(UnknownElevatorStatus.IN_SERVICE);

    var delivery = serviceDelivery(
      """
      <FacilityMonitoringDelivery>
        <ResponseTimestamp>2026-11-12T11:16:20Z</ResponseTimestamp>
        <Status>false</Status>
        <ErrorCondition><OtherError><ErrorText>Not available</ErrorText></OtherError></ErrorCondition>
      </FacilityMonitoringDelivery>
      """
    );

    assertEquals(Optional.empty(), mapper.map(delivery));
  }

  @Test
  void aServiceDeliveryWithStatusFalseIsNotMapped() {
    var mapper = new SiriFmElevatorMapper(UnknownElevatorStatus.IN_SERVICE);

    var xml = """
    <Siri xmlns="http://www.siri.org.uk/siri">
      <ServiceDelivery>
        <ResponseTimestamp>2026-11-12T11:16:20Z</ResponseTimestamp>
        <Status>false</Status>
        <FacilityMonitoringDelivery>
          <ResponseTimestamp>2026-11-12T11:16:20Z</ResponseTimestamp>
        </FacilityMonitoringDelivery>
      </ServiceDelivery>
    </Siri>
    """;

    assertEquals(Optional.empty(), mapper.map(parse(xml)));
  }

  @Test
  void keepPreviousIsNotChangedByASkippedDelivery() {
    var mapper = new SiriFmElevatorMapper(UnknownElevatorStatus.KEEP_PREVIOUS);

    mapper.map(siri(condition(A, "notAvailable", "lift")));
    mapper.map(serviceDelivery(""));

    assertEquals(
      List.of(outOfService(A)),
      mapper.map(siri(condition(A, "unknown", "lift"))).orElseThrow()
    );
  }

  private static String condition(String ref, String status, String accessFacility) {
    var features =
      accessFacility == null
        ? ""
        : "<Facility><Features><Feature><AccessFacility>%s</AccessFacility></Feature></Features></Facility>".formatted(
            accessFacility
          );
    var facilityStatus =
      status == null
        ? ""
        : "<FacilityStatus><Status>%s</Status></FacilityStatus>".formatted(status);
    return "<FacilityCondition><FacilityRef>%s</FacilityRef>%s%s</FacilityCondition>".formatted(
      ref,
      features,
      facilityStatus
    );
  }

  private static Siri siri(String... conditions) {
    return serviceDelivery(
      """
      <FacilityMonitoringDelivery>
        <ResponseTimestamp>2026-11-12T11:16:20Z</ResponseTimestamp>
        %s
      </FacilityMonitoringDelivery>
      """.formatted(Arrays.stream(conditions).collect(Collectors.joining()))
    );
  }

  private static Siri serviceDelivery(String content) {
    var xml = """
    <Siri xmlns="http://www.siri.org.uk/siri">
      <ServiceDelivery>
        <ResponseTimestamp>2026-11-12T11:16:20Z</ResponseTimestamp>
        %s
      </ServiceDelivery>
    </Siri>
    """.formatted(content);
    return parse(xml);
  }

  private static Siri parse(String xml) {
    try {
      return SiriXml.parseXml(xml);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
