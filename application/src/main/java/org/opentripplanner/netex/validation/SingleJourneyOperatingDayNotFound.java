package org.opentripplanner.netex.validation;

import org.entur.netex2.model.SingleJourney;
import org.jspecify.annotations.Nullable;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.netex.issues.ObjectNotFound;

class SingleJourneyOperatingDayNotFound extends AbstractHMapValidationRule<String, SingleJourney> {

  @Override
  public Status validate(SingleJourney journey) {
    return index.getOperatingDayById().containsKey(operatingDayRef(journey))
      ? Status.OK
      : Status.DISCARD;
  }

  @Override
  public DataImportIssue logMessage(String key, SingleJourney journey) {
    return new ObjectNotFound(
      "SingleJourney",
      journey.getId(),
      "OperatingDay",
      operatingDayRef(journey)
    );
  }

  @Nullable
  private static String operatingDayRef(SingleJourney journey) {
    var ref = journey.getOperatingDayRef();
    return ref == null ? null : ref.getRef();
  }
}
