package org.opentripplanner.netex.validation;

import java.util.List;
import org.entur.netex2.model.SingleJourney;
import org.entur.netex2.model.TargetPassingTime;
import org.entur.netex2.model.VehicleMeetingPointInPath;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.graph_builder.issue.api.Issue;

/**
 * A single journey must have a passing time for each point in its path, in the same order, and
 * each with an arrival or a departure time. A journey needs at least two points.
 */
class SingleJourneyPassingTimesMismatch extends AbstractHMapValidationRule<String, SingleJourney> {

  @Override
  public Status validate(SingleJourney journey) {
    List<VehicleMeetingPointInPath> points = pointsInPath(journey);
    List<TargetPassingTime> passingTimes = passingTimes(journey);

    if (points.size() < 2 || points.size() != passingTimes.size()) {
      return Status.DISCARD;
    }
    for (int i = 0; i < points.size(); i++) {
      if (!isPassingTimeAt(passingTimes.get(i), points.get(i))) {
        return Status.DISCARD;
      }
    }
    return Status.OK;
  }

  @Override
  public DataImportIssue logMessage(String key, SingleJourney journey) {
    return Issue.issue(
      "SingleJourneyPassingTimesMismatch",
      "SingleJourney %s is skipped. It must have a passing time, with an arrival or departure " +
        "time, for each of the two or more points in SingleJourneyPath %s, in the same order.",
      journey.getId(),
      journey.getSingleJourneyPathRef().getRef()
    );
  }

  private List<VehicleMeetingPointInPath> pointsInPath(SingleJourney journey) {
    var path = index.getSingleJourneyPathById().lookup(journey.getSingleJourneyPathRef().getRef());
    var points = path.getPointsInSequence();
    return points == null ? List.of() : points.getVehicleMeetingPointInPath();
  }

  private static List<TargetPassingTime> passingTimes(SingleJourney journey) {
    var passingTimes = journey.getDatedPassingTimes();
    return passingTimes == null ? List.of() : passingTimes.getTargetPassingTime();
  }

  private static boolean isPassingTimeAt(
    TargetPassingTime passingTime,
    VehicleMeetingPointInPath point
  ) {
    String pointRef = passingTime.getPointInJourneyPatternRef().getValue().getRef();
    boolean hasTime =
      passingTime.getAimedArrivalTime() != null || passingTime.getAimedDepartureTime() != null;
    return hasTime && pointRef.equals(point.getId());
  }
}
