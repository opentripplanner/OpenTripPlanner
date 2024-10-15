package org.opentripplanner.model.plan;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.framework.time.ServiceDateUtils;
import org.opentripplanner.transit.model.site.StopLocation;

/**
 * One leg of a trip -- that is, a temporally continuous piece of the journey that takes place on a
 * particular vehicle, which does run using a frequency-based schedule, rather than a timetable-
 * based schedule.
 */
public class FrequencyTransitLeg extends ScheduledTransitLeg {

  private final int frequencyHeadwayInSeconds;

  FrequencyTransitLeg(FrequencyTransitLegBuilder builder) {
    super(builder);
    this.frequencyHeadwayInSeconds = builder.frequencyHeadwayInSeconds();
  }

  @Override
  public Boolean getNonExactFrequency() {
    return frequencyHeadwayInSeconds != 0;
  }

  @Override
  public Integer getHeadway() {
    return frequencyHeadwayInSeconds;
  }

  @Override
  public boolean isPartiallySameTransitLeg(Leg other) {
    var same = super.isPartiallySameTransitLeg(other);
    // frequency-based trips have all the same trip id, so we have to check that the start times
    // are not equal
    if (other instanceof FrequencyTransitLeg frequencyTransitLeg) {
      var start = getTripTimes().getDepartureTime(0);
      var otherStart = frequencyTransitLeg.getTripTimes().getDepartureTime(0);
      return same && (start == otherStart);
    } else {
      return same;
    }
  }

  @Override
  public List<StopArrival> getIntermediateStops() {
    List<StopArrival> visits = new ArrayList<>();

    for (int i = boardStopPosInPattern + 1; i < alightStopPosInPattern; i++) {
      StopLocation stop = tripPattern.getStop(i);

      int arrivalTime = tripTimes.getArrivalTime(i);
      int departureTime = tripTimes.getDepartureTime(i) + frequencyHeadwayInSeconds;

      StopArrival visit = new StopArrival(
        Place.forStop(stop),
        LegTime.ofStatic(ServiceDateUtils.toZonedDateTime(serviceDate, zoneId, arrivalTime)),
        LegTime.ofStatic(ServiceDateUtils.toZonedDateTime(serviceDate, zoneId, departureTime)),
        i,
        tripTimes.gtfsSequenceOfStopIndex(i)
      );
      visits.add(visit);
    }
    return visits;
  }

  @Override
  public ScheduledTransitLeg withAccessibilityScore(Float score) {
    return new FrequencyTransitLegBuilder(this).withAccessibilityScore(score).build();
  }
}
