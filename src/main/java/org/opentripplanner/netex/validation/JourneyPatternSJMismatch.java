package org.opentripplanner.netex.validation;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.rutebanken.netex.model.JourneyPattern_VersionStructure;
import org.rutebanken.netex.model.PointInLinkSequence_VersionedChildStructure;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.StopPointInJourneyPattern;
import org.rutebanken.netex.model.StopUseEnumeration;

class JourneyPatternSJMismatch extends AbstractHMapValidationRule<String, ServiceJourney> {

  @Override
  public Status validate(ServiceJourney sj) {
    JourneyPattern_VersionStructure journeyPattern = index
      .getJourneyPatternsById()
      .lookup(getPatternId(sj));

    int nStopPointsInJourneyPattern = (int) journeyPattern
      .getPointsInSequence()
      .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern()
      .stream()
      .filter(JourneyPatternSJMismatch::stopsAtQuay)
      .count();

    int nTimetablePassingTimes = sj.getPassingTimes().getTimetabledPassingTime().size();

    return nStopPointsInJourneyPattern != nTimetablePassingTimes ? Status.DISCARD : Status.OK;
  }

  private static boolean stopsAtQuay(PointInLinkSequence_VersionedChildStructure point) {
    return switch (point) {
      case StopPointInJourneyPattern spijp -> spijp.getStopUse() != StopUseEnumeration.PASSTHROUGH;
      default -> true;
    };
  }

  @Override
  public DataImportIssue logMessage(String key, ServiceJourney sj) {
    return new StopPointsMismatch(sj.getId(), getPatternId(sj));
  }

  private String getPatternId(ServiceJourney sj) {
    return sj.getJourneyPatternRef().getValue().getRef();
  }

  private static class StopPointsMismatch implements DataImportIssue {

    private final String sjId;
    private final String patternId;

    public StopPointsMismatch(String sjId, String patternId) {
      this.sjId = sjId;
      this.patternId = patternId;
    }

    @Override
    public String getMessage() {
      return (
        "Mismatch in stop points between ServiceJourney and JourneyPattern. " +
        "ServiceJourney will be skipped. " +
        " ServiceJourney=" +
        sjId +
        ", JourneyPattern= " +
        patternId
      );
    }
  }
}
