package org.opentripplanner.netex.validation;

import org.entur.netex2.model.SingleJourney;
import org.jspecify.annotations.Nullable;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.graph_builder.issue.api.Issue;

/**
 * A single journey and its path are mapped together, so the path must be in the same file as the
 * journey.
 */
class SingleJourneyPathNotFound extends AbstractHMapValidationRule<String, SingleJourney> {

  @Override
  public Status validate(SingleJourney journey) {
    return index.getSingleJourneyPathById().localKeys().contains(pathRef(journey))
      ? Status.OK
      : Status.DISCARD;
  }

  @Override
  public DataImportIssue logMessage(String key, SingleJourney journey) {
    return Issue.issue(
      "SingleJourneyPathNotFound",
      "SingleJourney %s is skipped. SingleJourneyPath %s is not found in the file of the journey.",
      journey.getId(),
      pathRef(journey)
    );
  }

  @Nullable
  private static String pathRef(SingleJourney journey) {
    var ref = journey.getSingleJourneyPathRef();
    return ref == null ? null : ref.getRef();
  }
}
