package org.opentripplanner.ext.taxi.internal.graphbuilder;

import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;

/**
 * A trip in a taxi provider feed did not satisfy the data requirements for taxi trips
 * and was therefore skipped.
 */
public record TaxiTripSkipped(FeedScopedId tripId, String reason) implements DataImportIssue {
  private static final String REQUIREMENTS_URL =
    "https://docs.opentripplanner.org/en/latest/sandbox/Taxi/";

  @Override
  public String getMessage() {
    return "Skipping taxi trip %s. Reason: %s. See %s for the data requirements.".formatted(
      tripId,
      reason,
      REQUIREMENTS_URL
    );
  }
}
