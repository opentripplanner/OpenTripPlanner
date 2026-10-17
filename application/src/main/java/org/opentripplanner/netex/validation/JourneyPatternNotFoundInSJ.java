package org.opentripplanner.netex.validation;

import org.entur.netex2.model.ServiceJourney;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.netex.issues.ObjectNotFound;

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
