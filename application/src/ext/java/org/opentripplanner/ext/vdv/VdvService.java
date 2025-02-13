package org.opentripplanner.ext.vdv;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.opentripplanner.model.StopTimesInPattern;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.routing.stoptimes.ArrivalDeparture;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.service.TransitService;

public class VdvService {

  private final TransitService transitService;

  public VdvService(TransitService transitService) {
    this.transitService = transitService;
  }

  public List<TripTimeOnDate> findStopTimesInPattern(String stopId, int numResults) {
    var stop = transitService.getRegularStop(FeedScopedId.parse("tampere:" + stopId));
    List<StopTimesInPattern> stopTimesInPatterns = transitService.findStopTimesInPattern(
      stop,
      Instant.now(),
      Duration.ofHours(2),
      numResults,
      ArrivalDeparture.BOTH,
      true
    );

    return stopTimesInPatterns
      .stream()
      .flatMap(st -> st.times.stream())
      .sorted(Comparator.comparingLong(TripTimeOnDate::getRealtimeDeparture))
      .toList();
  }
}
