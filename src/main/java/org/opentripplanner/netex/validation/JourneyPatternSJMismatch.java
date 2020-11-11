package org.opentripplanner.netex.validation;

import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.ServiceJourney;

class JourneyPatternSJMismatch extends AbstractHMapValidationRule<String, ServiceJourney> {

  @Override
  public Status validate(String key, ServiceJourney sj) {
    JourneyPattern journeyPattern = index.getJourneyPatternsById().lookup(getPatternId(sj));

    int nStopPointsInJourneyPattern = journeyPattern
        .getPointsInSequence()
        .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern()
        .size();

    int nTimetablePassingTimes = sj.getPassingTimes().getTimetabledPassingTime().size();

    return nStopPointsInJourneyPattern != nTimetablePassingTimes
        ?  Status.DISCARD
        : Status.OK;
  }

  @Override
  public String logMessage(String key, ServiceJourney sj) {
    return "Mismatch in stop points between ServiceJourney and JourneyPattern. "
        + "ServiceJourney will be skipped. "
        + " ServiceJourney=" + sj.getId()
        + ", JourneyPattern= " +getPatternId(sj);
  }

  private String getPatternId(ServiceJourney sj) {
    return sj.getJourneyPatternRef().getValue().getRef();
  }
}
