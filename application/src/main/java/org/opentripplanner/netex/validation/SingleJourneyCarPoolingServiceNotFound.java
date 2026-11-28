package org.opentripplanner.netex.validation;

import org.entur.netex2.model.SingleJourney;
import org.jspecify.annotations.Nullable;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.graph_builder.issue.api.Issue;

/**
 * Only the single journeys of a car pooling service are imported. A single journey can also belong
 * to another kind of service, like a taxi service.
 */
class SingleJourneyCarPoolingServiceNotFound
  extends AbstractHMapValidationRule<String, SingleJourney>
{

  @Override
  public Status validate(SingleJourney journey) {
    return index.getCarPoolingServiceById().containsKey(serviceRef(journey))
      ? Status.OK
      : Status.DISCARD;
  }

  @Override
  public DataImportIssue logMessage(String key, SingleJourney journey) {
    return Issue.issue(
      "SingleJourneyCarPoolingServiceNotFound",
      "SingleJourney %s is skipped. Only the journeys of a CarPoolingService in the import are " +
        "mapped, the service of the journey is %s.",
      journey.getId(),
      serviceRef(journey)
    );
  }

  @Nullable
  private static String serviceRef(SingleJourney journey) {
    var ref = journey.getCommonVehicleServiceRef();
    return ref == null ? null : ref.getValue().getRef();
  }
}
