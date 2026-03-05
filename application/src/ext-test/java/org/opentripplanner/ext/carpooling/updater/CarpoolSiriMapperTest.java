package org.opentripplanner.ext.carpooling.updater;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.ext.carpooling.CarpoolEstimatedVehicleJourneyData.arrivalIsAfterDepartureTime;
import static org.opentripplanner.ext.carpooling.CarpoolEstimatedVehicleJourneyData.lessThanTwoStops;
import static org.opentripplanner.ext.carpooling.CarpoolEstimatedVehicleJourneyData.minimalCompleteJourney;
import static org.opentripplanner.ext.carpooling.CarpoolEstimatedVehicleJourneyData.stopTimesAreOutOfOrder;

import org.junit.jupiter.api.Test;
import uk.org.siri.siri21.EstimatedCall;

public class CarpoolSiriMapperTest {

  private final CarpoolSiriMapper mapper = new CarpoolSiriMapper();

  @Test
  void mapSiriToCarpoolTrip_arrivalIsAfterDepartureTime_trowsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () ->
      mapper.mapSiriToCarpoolTrip(arrivalIsAfterDepartureTime())
    );
  }

  @Test
  void mapSiriToCarpoolTrip_lessThanTwoStops_trowsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () ->
      mapper.mapSiriToCarpoolTrip(lessThanTwoStops())
    );
  }

  @Test
  void mapSiriToCarpoolTrip_minimalData_mapsOk() {
    var journey = minimalCompleteJourney();
    var mapped = mapper.mapSiriToCarpoolTrip(journey);

    var expectedStartTime = journey
      .getEstimatedCalls()
      .getEstimatedCalls()
      .stream()
      .findFirst()
      .map(EstimatedCall::getAimedDepartureTime)
      .orElseThrow();
    var expectedEndTime = journey
      .getEstimatedCalls()
      .getEstimatedCalls()
      .stream()
      .reduce((a, b) -> b)
      .map(EstimatedCall::getAimedArrivalTime)
      .orElseThrow();
    assertEquals(expectedStartTime, mapped.startTime());
    assertEquals(expectedEndTime, mapped.endTime());

    var startName = journey
      .getEstimatedCalls()
      .getEstimatedCalls()
      .getFirst()
      .getStopPointNames()
      .getFirst()
      .getValue();
    var endName = journey
      .getEstimatedCalls()
      .getEstimatedCalls()
      .getLast()
      .getStopPointNames()
      .getFirst()
      .getValue();
    assertEquals("First stop", startName);
    assertEquals("Last stop", endName);
  }

  @Test
  void mapSiriToCarpoolTrip_stopTimesAreOutOfOrder_trowsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () ->
      mapper.mapSiriToCarpoolTrip(stopTimesAreOutOfOrder())
    );
  }
}
