package org.opentripplanner.updater.trip.siri;

/**
 * Adapter for normalizing EstimatedVehicleJourneyCode to proper NeTEx IDs.
 * Ensures Trip uses ServiceJourney entity type and TripOnServiceDate uses DatedServiceJourney.
 * <p>
 * Expected format: {@code codespace:entityType:sequenceNumber} (e.g., {@code RUT:ServiceJourney:1234}).
 * Transforms IDs with 3 or more colon-separated parts where the second part is
 * "ServiceJourney" or "DatedServiceJourney". Other formats are returned unchanged.
 */
class EstimatedVehicleJourneyCodeAdapter {

  private static final String SERVICE_JOURNEY = "ServiceJourney";
  private static final String DATED_SERVICE_JOURNEY = "DatedServiceJourney";

  private final String estimatedVehicleJourneyCode;
  private final String[] parts;

  public EstimatedVehicleJourneyCodeAdapter(String estimatedVehicleJourneyCode) {
    this.estimatedVehicleJourneyCode = estimatedVehicleJourneyCode;
    this.parts = estimatedVehicleJourneyCode.split(":");
  }

  /**
   * Get code normalized to ServiceJourney entity type (for Trip entities).
   * If code has DatedServiceJourney as middle part, swaps to ServiceJourney.
   */
  public String getServiceJourneyId() {
    return normalizeEntityType(DATED_SERVICE_JOURNEY, SERVICE_JOURNEY);
  }

  /**
   * Get code normalized to DatedServiceJourney entity type (for TripOnServiceDate entities).
   * If code has ServiceJourney as middle part, swaps to DatedServiceJourney.
   */
  public String getDatedServiceJourneyId() {
    return normalizeEntityType(SERVICE_JOURNEY, DATED_SERVICE_JOURNEY);
  }

  private String normalizeEntityType(String sourceType, String targetType) {
    if (parts.length >= 3 && parts[1].equals(sourceType)) {
      String remainder = String.join(":", java.util.Arrays.copyOfRange(parts, 2, parts.length));
      return parts[0] + ":" + targetType + ":" + remainder;
    }
    return estimatedVehicleJourneyCode;
  }
}
