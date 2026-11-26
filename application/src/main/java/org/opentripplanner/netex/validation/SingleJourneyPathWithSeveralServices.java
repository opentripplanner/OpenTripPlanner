package org.opentripplanner.netex.validation;

import java.util.HashMap;
import java.util.Map;
import org.entur.netex2.model.SingleJourney;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.graph_builder.issue.api.Issue;

/**
 * A single journey path becomes a trip pattern, which has one route. So all the journeys following
 * a path must belong to the same car pooling service, the service of the first journey found.
 */
class SingleJourneyPathWithSeveralServices
  extends AbstractHMapValidationRule<String, SingleJourney>
{

  private final Map<String, String> serviceIdByPathId = new HashMap<>();

  @Override
  public Status validate(SingleJourney journey) {
    String serviceId = journey.getCommonVehicleServiceRef().getValue().getRef();
    String serviceOfPath = serviceIdByPathId.putIfAbsent(pathId(journey), serviceId);
    return serviceOfPath == null || serviceOfPath.equals(serviceId) ? Status.OK : Status.DISCARD;
  }

  @Override
  public DataImportIssue logMessage(String key, SingleJourney journey) {
    return Issue.issue(
      "SingleJourneyPathWithSeveralServices",
      "SingleJourney %s is skipped. SingleJourneyPath %s is used by CarPoolingService %s, and a " +
        "path can only have the journeys of one service.",
      journey.getId(),
      pathId(journey),
      serviceIdByPathId.get(pathId(journey))
    );
  }

  private static String pathId(SingleJourney journey) {
    return journey.getSingleJourneyPathRef().getRef();
  }
}
