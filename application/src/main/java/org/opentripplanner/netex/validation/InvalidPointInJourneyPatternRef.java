package org.opentripplanner.netex.validation;

import java.util.stream.Collectors;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.graph_builder.issue.api.Issue;
import org.rutebanken.netex.model.EntityStructure;
import org.rutebanken.netex.model.ServiceJourney;

/**
 * Checks that all refs to stop points in journey pattern can be found in the journey pattern.
 * <p>
 * This is a more specific check than {@link JourneyPatternSJMismatch} which only checks that
 * the number of points is equal.
 */
class InvalidPointInJourneyPatternRef extends AbstractHMapValidationRule<String, ServiceJourney> {

  @Override
  public Status validate(ServiceJourney sj) {
    var journeyPattern = index.getJourneyPatternsById().lookup(getPatternId(sj));

    var stopPointsInPattern = journeyPattern
      .getPointsInSequence()
      .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern()
      .stream()
      .map(EntityStructure::getId)
      .collect(Collectors.toSet());

    var stopPointRefs = sj
      .getPassingTimes()
      .getTimetabledPassingTime()
      .stream()
      .map(p -> p.getPointInJourneyPatternRef().getValue().getRef())
      .toList();

    if (stopPointsInPattern.containsAll(stopPointRefs)) {
      return Status.OK;
    } else {
      return Status.DISCARD;
    }
  }

  @Override
  public DataImportIssue logMessage(String key, ServiceJourney sj) {
    return Issue.issue(
      "InvalidPointInJourneyPatternRef",
      "ServiceJourney %s contains invalid PointInJourneyPatternRef".formatted(getPatternId(sj))
    );
  }

  private String getPatternId(ServiceJourney sj) {
    return sj.getJourneyPatternRef().getValue().getRef();
  }
}
