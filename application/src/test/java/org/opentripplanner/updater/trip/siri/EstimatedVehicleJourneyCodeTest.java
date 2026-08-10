package org.opentripplanner.updater.trip.siri;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class EstimatedVehicleJourneyCodeTest {

  @ParameterizedTest
  @CsvSource(
    {
      "1234, 1234",
      "RUT:ServiceJourney:1234, RUT:ServiceJourney:1234",
      "RUT:DatedServiceJourney:1234, RUT:ServiceJourney:1234",
      "RUT:Unknown:1234, RUT:Unknown:1234",
      "RUT:ServiceJourney:1234:extra, RUT:ServiceJourney:1234:extra",
      "RUT:DatedServiceJourney:1234:extra, RUT:ServiceJourney:1234:extra",
    }
  )
  void asServiceJourneyId(String code, String expected) {
    assertEquals(expected, new EstimatedVehicleJourneyCode(code).asServiceJourneyId());
  }

  @ParameterizedTest
  @CsvSource(
    {
      "1234, 1234",
      "RUT:ServiceJourney:1234, RUT:DatedServiceJourney:1234",
      "RUT:DatedServiceJourney:1234, RUT:DatedServiceJourney:1234",
      "RUT:Unknown:1234, RUT:Unknown:1234",
      "RUT:ServiceJourney:1234:extra, RUT:DatedServiceJourney:1234:extra",
      "RUT:DatedServiceJourney:1234:extra, RUT:DatedServiceJourney:1234:extra",
      "\"\" , \"\"",
      "::,::",
      "::ID, ::ID",
      ":ServiceJourney:ID, :DatedServiceJourney:ID",
      "NAME_SPACE::, NAME_SPACE::",
    }
  )
  void asDatedServiceJourneyId(String code, String expected) {
    assertEquals(expected, new EstimatedVehicleJourneyCode(code).asDatedServiceJourneyId());
  }
}
