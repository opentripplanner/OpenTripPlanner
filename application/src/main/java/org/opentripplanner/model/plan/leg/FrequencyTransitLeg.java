package org.opentripplanner.model.plan.leg;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.utils.time.ServiceDateUtils;

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
  public FrequencyTransitLegBuilder copyOf() {
    return new FrequencyTransitLegBuilder(this);
  }

  @Override
  public Boolean isNonExactFrequency() {
    return frequencyHeadwayInSeconds != 0;
  }

  @Override
  public Integer headway() {
    return frequencyHeadwayInSeconds;
  }

  @Override
  public boolean isPartiallySameTransitLeg(Leg other) {
    var same = super.isPartiallySameTransitLeg(other);
    // frequency-based trips have all the same trip id, so we have to check that the start times
    // are not equal
    if (other instanceof FrequencyTransitLeg frequencyTransitLeg) {
      var start = tripTimes().getDepartureTime(0);
      var otherStart = frequencyTransitLeg.tripTimes().getDepartureTime(0);
      return same && (start == otherStart);
    } else {
      return same;
    }
  }

  @Override
  public List<StopArrival> listIntermediateStops() {
    List<StopArrival> visits = new ArrayList<>();

    for (int i = boardStopPosInPattern + 1; i < alightStopPosInPattern; i++) {
      StopLocation stop = tripPattern().getStop(i);

      int arrivalTime = tripTimes().getArrivalTime(i);
      int departureTime = tripTimes().getDepartureTime(i) + frequencyHeadwayInSeconds;

      StopArrival visit = new StopArrival(
        Place.forStop(stop),
        LegCallTime.ofStatic(
          ServiceDateUtils.toZonedDateTime(serviceDate(), zoneId(), arrivalTime)
        ),
        LegCallTime.ofStatic(
          ServiceDateUtils.toZonedDateTime(serviceDate(), zoneId(), departureTime)
        ),
        i,
        tripTimes().gtfsSequenceOfStopIndex(i)
      );
      visits.add(visit);
    }
    return visits;
  }
}
