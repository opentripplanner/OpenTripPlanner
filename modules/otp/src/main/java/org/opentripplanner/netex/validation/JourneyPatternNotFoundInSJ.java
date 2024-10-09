package org.opentripplanner.netex.validation;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.netex.issues.ObjectNotFound;
import org.rutebanken.netex.model.ServiceJourney;

class JourneyPatternNotFoundInSJ extends AbstractHMapValidationRule<String, ServiceJourney> {

  @Override
  public Status validate(ServiceJourney sj) {
    return index.getJourneyPatternsById().lookup(getPatternId(sj)) == null
      ? Status.DISCARD
      : Status.OK;
  }

  @Override
  public DataImportIssue logMessage(String key, ServiceJourney sj) {
    return new ObjectNotFound("ServiceJourney", sj.getId(), "JourneyPattern", getPatternId(sj));
  }

  private String getPatternId(ServiceJourney sj) {
    return sj.getJourneyPatternRef().getValue().getRef();
  }
}
