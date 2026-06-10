package org.opentripplanner.ext.carpooling.updater;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.ext.carpooling.CarpoolEstimatedVehicleJourneyData.arrivalIsAfterDepartureTime;
import static org.opentripplanner.ext.carpooling.CarpoolEstimatedVehicleJourneyData.journeyWithAllButOneCallCancelled;
import static org.opentripplanner.ext.carpooling.CarpoolEstimatedVehicleJourneyData.journeyWithCancelledIntermediateCall;
import static org.opentripplanner.ext.carpooling.CarpoolEstimatedVehicleJourneyData.journeyWithDifferentCapacitiesPerCall;
import static org.opentripplanner.ext.carpooling.CarpoolEstimatedVehicleJourneyData.journeyWithLatestExpectedArrivalTime;
import static org.opentripplanner.ext.carpooling.CarpoolEstimatedVehicleJourneyData.journeyWithLatestExpectedArrivalTimeAimedOnly;
import static org.opentripplanner.ext.carpooling.CarpoolEstimatedVehicleJourneyData.journeyWithOnboardCounts;
import static org.opentripplanner.ext.carpooling.CarpoolEstimatedVehicleJourneyData.journeyWithPerStopLatestExpectedArrivalTimes;
import static org.opentripplanner.ext.carpooling.CarpoolEstimatedVehicleJourneyData.journeyWithPublicContact;
import static org.opentripplanner.ext.carpooling.CarpoolEstimatedVehicleJourneyData.journeyWithTotalCapacity;
import static org.opentripplanner.ext.carpooling.CarpoolEstimatedVehicleJourneyData.lessThanTwoStops;
import static org.opentripplanner.ext.carpooling.CarpoolEstimatedVehicleJourneyData.minimalCompleteJourney;
import static org.opentripplanner.ext.carpooling.CarpoolEstimatedVehicleJourneyData.minimalCompleteJourneyWithPolygon;
import static org.opentripplanner.ext.carpooling.CarpoolEstimatedVehicleJourneyData.stopTimesAreOutOfOrder;
import static org.opentripplanner.ext.carpooling.CarpoolEstimatedVehicleJourneyData.tripHasAimedTimesOnly;
import static org.opentripplanner.ext.carpooling.CarpoolEstimatedVehicleJourneyData.tripHasExpectedTimesOnly;
import static org.opentripplanner.ext.carpooling.model.CarpoolStop.DEFAULT_DEVIATION_BUDGET;
import static org.opentripplanner.ext.carpooling.model.CarpoolStop.DEFAULT_ONBOARD_COUNT;
import static org.opentripplanner.ext.carpooling.model.CarpoolTrip.DEFAULT_TOTAL_CAPACITY;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import uk.org.siri.siri21.EstimatedCall;

public class CarpoolSiriMapperTest {

  private final CarpoolSiriMapper mapper = new CarpoolSiriMapper();

  @Test
  void mapSiriToCarpoolTrip_arrivalIsAfterDepartureTime_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () ->
      mapper.mapSiriToCarpoolTrip(arrivalIsAfterDepartureTime())
    );
  }

  @Test
  void mapSiriToCarpoolTrip_lessThanTwoStops_throwsIllegalArgumentException() {
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
  void mapSiriToCarpoolTrip_minimalDataUsingPolygonStops_mapsOk() {
    var journey = minimalCompleteJourneyWithPolygon();
    var mapped = mapper.mapSiriToCarpoolTrip(journey);

    var firstStop = mapped.stops().getFirst();
    var lastStop = mapped.stops().getLast();

    assertNotNull(firstStop.getCoordinate());
    assertNotNull(lastStop.getCoordinate());
  }

  @Test
  void mapSiriToCarpoolTrip_tripHasOnlyAimedTimes_mapsOk() {
    var journey = tripHasAimedTimesOnly();
    var mapped = mapper.mapSiriToCarpoolTrip(journey);

    assertNotNull(mapped.startTime());
    assertNotNull(mapped.endTime());
  }

  @Test
  void mapSiriToCarpoolTrip_tripHasOnlyExpectedTimes_mapsOk() {
    var journey = tripHasExpectedTimesOnly();
    var mapped = mapper.mapSiriToCarpoolTrip(journey);

    assertNotNull(mapped.startTime());
    assertNotNull(mapped.endTime());
  }

  @Test
  void mapSiriToCarpoolTrip_stopTimesAreOutOfOrder_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () ->
      mapper.mapSiriToCarpoolTrip(stopTimesAreOutOfOrder())
    );
  }

  // -- extractTotalCapacity tests --

  @Test
  void mapSiriToCarpoolTrip_noCapacityData_returnsDefaultCapacity() {
    var mapped = mapper.mapSiriToCarpoolTrip(minimalCompleteJourney());
    assertEquals(DEFAULT_TOTAL_CAPACITY, mapped.totalCapacity());
  }

  @Test
  void mapSiriToCarpoolTrip_withCapacityData_usesProvidedCapacity() {
    var mapped = mapper.mapSiriToCarpoolTrip(journeyWithTotalCapacity(3));
    assertEquals(3, mapped.totalCapacity());
  }

  @Test
  void mapSiriToCarpoolTrip_zeroCapacity_returnsDefaultCapacity() {
    var mapped = mapper.mapSiriToCarpoolTrip(journeyWithTotalCapacity(0));
    assertEquals(DEFAULT_TOTAL_CAPACITY, mapped.totalCapacity());
  }

  @Test
  void mapSiriToCarpoolTrip_negativeCapacity_returnsDefaultCapacity() {
    var mapped = mapper.mapSiriToCarpoolTrip(journeyWithTotalCapacity(-1));
    assertEquals(DEFAULT_TOTAL_CAPACITY, mapped.totalCapacity());
  }

  @Test
  void mapSiriToCarpoolTrip_differentCapacitiesPerCall_usesFirstValue() {
    var mapped = mapper.mapSiriToCarpoolTrip(journeyWithDifferentCapacitiesPerCall(3, 7));
    assertEquals(3, mapped.totalCapacity());
  }

  @Test
  void mapSiriToCarpoolTrip_consistentCapacitiesPerCall_usesValue() {
    var mapped = mapper.mapSiriToCarpoolTrip(journeyWithDifferentCapacitiesPerCall(4, 4));
    assertEquals(4, mapped.totalCapacity());
  }

  // -- extractOnboardCount tests --

  @Test
  void mapSiriToCarpoolTrip_noOccupancyData_returnsDefaultOnboardCount() {
    var mapped = mapper.mapSiriToCarpoolTrip(minimalCompleteJourney());
    for (var stop : mapped.stops()) {
      assertEquals(DEFAULT_ONBOARD_COUNT, stop.getOnboardCount());
    }
  }

  @Test
  void mapSiriToCarpoolTrip_withOccupancyData_usesProvidedOnboardCount() {
    var mapped = mapper.mapSiriToCarpoolTrip(journeyWithOnboardCounts(2, 3));
    assertEquals(2, mapped.stops().getFirst().getOnboardCount());
    assertEquals(3, mapped.stops().getLast().getOnboardCount());
  }

  @Test
  void mapSiriToCarpoolTrip_zeroOnboardCount_returnsDefaultOnboardCount() {
    var mapped = mapper.mapSiriToCarpoolTrip(journeyWithOnboardCounts(0, 0));
    for (var stop : mapped.stops()) {
      assertEquals(DEFAULT_ONBOARD_COUNT, stop.getOnboardCount());
    }
  }

  @Test
  void mapSiriToCarpoolTrip_negativeOnboardCount_returnsDefaultOnboardCount() {
    var mapped = mapper.mapSiriToCarpoolTrip(journeyWithOnboardCounts(-1, -1));
    for (var stop : mapped.stops()) {
      assertEquals(DEFAULT_ONBOARD_COUNT, stop.getOnboardCount());
    }
  }

  // -- extractDeviationBudget tests --

  @Test
  void mapSiriToCarpoolTrip_noLatestExpectedArrivalTime_returnsDefaultDeviationBudget() {
    var mapped = mapper.mapSiriToCarpoolTrip(minimalCompleteJourney());
    assertEquals(Duration.ZERO, mapped.stops().getFirst().getDeviationBudget());
    assertEquals(DEFAULT_DEVIATION_BUDGET, mapped.stops().getLast().getDeviationBudget());
  }

  @Test
  void mapSiriToCarpoolTrip_withLatestExpectedArrivalTime_computesDeviationBudget() {
    var mapped = mapper.mapSiriToCarpoolTrip(journeyWithLatestExpectedArrivalTime(0, 10));
    var lastStop = mapped.stops().getLast();
    assertEquals(Duration.ofMinutes(10), lastStop.getDeviationBudget());
  }

  @Test
  void mapSiriToCarpoolTrip_withLatestExpectedArrivalTimeNoExpected_usesAimedArrivalTime() {
    var mapped = mapper.mapSiriToCarpoolTrip(journeyWithLatestExpectedArrivalTimeAimedOnly(20));
    var lastStop = mapped.stops().getLast();
    assertEquals(Duration.ofMinutes(20), lastStop.getDeviationBudget());
  }

  @Test
  void mapSiriToCarpoolTrip_originStop_hasZeroDeviationBudget() {
    var mapped = mapper.mapSiriToCarpoolTrip(journeyWithLatestExpectedArrivalTime(0, 10));
    assertEquals(Duration.ZERO, mapped.stops().getFirst().getDeviationBudget());
  }

  @Test
  void mapSiriToCarpoolTrip_latestBeforeExpected_returnsZeroDeviationBudget() {
    // latestExpectedArrival is before expectedArrival — schedule has slipped past commitment,
    // no further deviation is acceptable
    var mapped = mapper.mapSiriToCarpoolTrip(journeyWithLatestExpectedArrivalTime(10, 5));
    var lastStop = mapped.stops().getLast();
    assertEquals(Duration.ZERO, lastStop.getDeviationBudget());
  }

  // -- cancellation tests --

  @Test
  void mapSiriToCarpoolTrip_intermediateCallCancelled_dropsThatCall() {
    var mapped = mapper.mapSiriToCarpoolTrip(journeyWithCancelledIntermediateCall());

    assertNotNull(mapped);
    var stopIds = mapped
      .stops()
      .stream()
      .map(s -> s.getId().getId())
      .toList();
    assertEquals(List.of("unittest_trip_origin", "unittest_trip_destination"), stopIds);
  }

  @Test
  void mapSiriToCarpoolTrip_fewerThanTwoActiveCalls_returnsNull() {
    assertNull(mapper.mapSiriToCarpoolTrip(journeyWithAllButOneCallCancelled()));
  }

  @Test
  void mapSiriToCarpoolTrip_multiStopWithDifferingBudgets_eachStopHasOwnBudget() {
    // 3-stop journey. Intermediate arrives at +20 with latest +23 (3 min slack),
    // last arrives at +45 with latest +55 (10 min slack).
    var mapped = mapper.mapSiriToCarpoolTrip(
      journeyWithPerStopLatestExpectedArrivalTimes(20, 23, 45, 55)
    );
    assertEquals(3, mapped.stops().size());
    assertEquals(Duration.ZERO, mapped.stops().get(0).getDeviationBudget());
    assertEquals(Duration.ofMinutes(3), mapped.stops().get(1).getDeviationBudget());
    assertEquals(Duration.ofMinutes(10), mapped.stops().get(2).getDeviationBudget());
  }

  // -- publicContact tests --

  @Test
  void mapSiriToCarpoolTrip_withPublicContact_mapsContactInformation() {
    var journey = journeyWithPublicContact("+4712345678", "https://example.com/book");
    var mapped = mapper.mapSiriToCarpoolTrip(journey);

    assertNotNull(mapped.publicContactInformation());
    assertEquals("+4712345678", mapped.publicContactInformation().getPhoneNumber());
    assertEquals("https://example.com/book", mapped.publicContactInformation().getBookingUrl());
  }

  @Test
  void mapSiriToCarpoolTrip_withoutPublicContact_contactInformationIsNull() {
    var journey = minimalCompleteJourney();
    var mapped = mapper.mapSiriToCarpoolTrip(journey);

    assertNull(mapped.publicContactInformation());
  }
}
