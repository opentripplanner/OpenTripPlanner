package org.opentripplanner.ext.taxizone.graphbuilder;

import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;

/**
 * A trip in a taxi zone provider feed did not satisfy the data requirements for taxi zones
 * and was therefore skipped.
 */
public record TaxiZoneTripSkipped(FeedScopedId tripId, String reason) implements DataImportIssue {
  private static final String REQUIREMENTS = """
  Taxi zone trips must satisfy all of the following requirements:
    1. The trip must be an unscheduled GTFS Flex trip (UnscheduledTrip).
    2. The trip's route mode must be TAXI (GTFS route_type 1500-1599).
    3. Stops must not have a time restriction: start_pickup_dropoff_window /
       end_pickup_dropoff_window must not be set, or must span the full day
       (0:00:00-24:00:00).
    4. The trip must have exactly 2 stop times: one pickup stop and one drop-off stop.
    5. Both stop times must reference the same GTFS Flex area (location_id) with a geometry.
    6. pickup_type at stop 0 and drop_off_type at stop 1 must both be 2 (CALL_AGENCY).\
  """;

  @Override
  public String getMessage() {
    return "Skipping taxi zone trip %s.\n%s\nReason this trip was skipped: %s".formatted(
      tripId,
      REQUIREMENTS,
      reason
    );
  }
}
