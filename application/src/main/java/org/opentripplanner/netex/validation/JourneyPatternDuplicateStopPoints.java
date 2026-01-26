package org.opentripplanner.netex.validation;

import java.util.HashSet;
import java.util.Set;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.graph_builder.issue.api.Issue;
import org.rutebanken.netex.model.EntityStructure;
import org.rutebanken.netex.model.JourneyPattern_VersionStructure;
import org.rutebanken.netex.model.ServiceJourney;

/**
 * Validates that a JourneyPattern does not contain duplicate StopPointInJourneyPattern IDs.
 * Duplicate stop point IDs in a journey pattern indicate invalid NeTEx data and will cause
 * failures when creating lookup maps.
 */
class JourneyPatternDuplicateStopPoints extends AbstractHMapValidationRule<String, ServiceJourney> {

  private String duplicateStopPointId;
  private String journeyPatternId;

  @Override
  public Status validate(ServiceJourney sj) {
    journeyPatternId = sj.getJourneyPatternRef().getValue().getRef();
    JourneyPattern_VersionStructure journeyPattern = index
      .getJourneyPatternsById()
      .lookup(journeyPatternId);

    Set<String> seenIds = new HashSet<>();

    for (var point : journeyPattern
      .getPointsInSequence()
      .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern()) {
      String pointId = ((EntityStructure) point).getId();
      if (!seenIds.add(pointId)) {
        duplicateStopPointId = pointId;
        return Status.DISCARD;
      }
    }

    return Status.OK;
  }

  @Override
  public DataImportIssue logMessage(String key, ServiceJourney sj) {
    return Issue.issue(
      "JourneyPatternDuplicateStopPoints",
      "JourneyPattern contains duplicate StopPointInJourneyPattern. " +
        "ServiceJourney will be skipped. " +
        "ServiceJourney=%s, JourneyPattern=%s, Duplicate StopPointInJourneyPattern=%s",
      sj.getId(),
      journeyPatternId,
      duplicateStopPointId
    );
  }
}
