package org.opentripplanner.netex.validation;

import org.rutebanken.netex.model.ServiceJourney;

class JourneyPatternNotFoundInSJ extends AbstractHMapValidationRule<String, ServiceJourney> {

  @Override
  public Status validate(String key, ServiceJourney sj) {
    return index.getJourneyPatternsById().lookup(getPatternId(sj)) == null
        ?  Status.DISCARD
        : Status.OK;
  }

  @Override
  public String logMessage(String key, ServiceJourney sj) {
    return "JourneyPattern not found for ServiceJourney."
        + " ServiceJourney=" + sj.getId()
        + ", JourneyPattern id= " +getPatternId(sj);
  }

  private String getPatternId(ServiceJourney sj) {
    return sj.getJourneyPatternRef().getValue().getRef();
  }
}
