package org.opentripplanner.updater.trip.siri;

import java.util.Arrays;

/**
 * The {@code EstimatedVehicleJourneyCode} of a SIRI estimated journey, able to present itself as
 * either a {@code ServiceJourney} id (identifying the added {@link
 * org.opentripplanner.transit.model.timetable.Trip}) or a {@code DatedServiceJourney} id
 * (identifying the added {@link org.opentripplanner.transit.model.timetable.TripOnServiceDate}).
 * <p>
 * Expected format: {@code codespace:entityType:sequenceNumber} (e.g. {@code RUT:ServiceJourney:1234}).
 * Codes with 3 or more colon-separated parts whose second part is {@code ServiceJourney} or
 * {@code DatedServiceJourney} are normalized to the requested entity type; any other format is
 * returned unchanged.
 */
class EstimatedVehicleJourneyCode {

  private static final String SERVICE_JOURNEY = "ServiceJourney";
  private static final String DATED_SERVICE_JOURNEY = "DatedServiceJourney";

  private final String code;
  private final String[] parts;

  EstimatedVehicleJourneyCode(String code) {
    this.code = code;
    this.parts = code.split(":");
  }

  /**
   * This code viewed as a {@code ServiceJourney} id. A code in {@code DatedServiceJourney} form is
   * swapped to {@code ServiceJourney}.
   */
  String asServiceJourneyId() {
    return normalizeEntityType(DATED_SERVICE_JOURNEY, SERVICE_JOURNEY);
  }

  /**
   * This code viewed as a {@code DatedServiceJourney} id. A code in {@code ServiceJourney} form is
   * swapped to {@code DatedServiceJourney}.
   */
  String asDatedServiceJourneyId() {
    return normalizeEntityType(SERVICE_JOURNEY, DATED_SERVICE_JOURNEY);
  }

  private String normalizeEntityType(String sourceType, String targetType) {
    if (parts.length >= 3 && parts[1].equals(sourceType)) {
      String remainder = String.join(":", Arrays.copyOfRange(parts, 2, parts.length));
      return parts[0] + ":" + targetType + ":" + remainder;
    }
    return code;
  }
}
