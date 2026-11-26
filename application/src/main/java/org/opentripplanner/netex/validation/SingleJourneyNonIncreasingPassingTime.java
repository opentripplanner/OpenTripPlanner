package org.opentripplanner.netex.validation;

import static org.opentripplanner.netex.support.ServiceJourneyHelper.elapsedTimeSinceMidnight;

import java.util.ArrayList;
import java.util.List;
import org.entur.netex2.model.SingleJourney;
import org.entur.netex2.model.TargetPassingTime;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.graph_builder.issue.api.Issue;

/**
 * The arrival and departure times of a single journey can not go back in time.
 */
class SingleJourneyNonIncreasingPassingTime
  extends AbstractHMapValidationRule<String, SingleJourney>
{

  @Override
  public Status validate(SingleJourney journey) {
    List<Integer> times = new ArrayList<>();
    for (TargetPassingTime it : journey.getDatedPassingTimes().getTargetPassingTime()) {
      if (it.getAimedArrivalTime() != null) {
        times.add(elapsedTimeSinceMidnight(it.getAimedArrivalTime(), it.getArrivalDayOffset()));
      }
      if (it.getAimedDepartureTime() != null) {
        times.add(elapsedTimeSinceMidnight(it.getAimedDepartureTime(), it.getDepartureDayOffset()));
      }
    }

    for (int i = 1; i < times.size(); i++) {
      if (times.get(i) < times.get(i - 1)) {
        return Status.DISCARD;
      }
    }
    return Status.OK;
  }

  @Override
  public DataImportIssue logMessage(String key, SingleJourney journey) {
    return Issue.issue(
      "SingleJourneyNonIncreasingPassingTime",
      "SingleJourney %s is skipped. Its passing times go back in time.",
      journey.getId()
    );
  }
}
