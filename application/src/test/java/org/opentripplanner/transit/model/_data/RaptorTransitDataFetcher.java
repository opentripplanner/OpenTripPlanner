package org.opentripplanner.transit.model._data;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripPatternForDate;
import org.opentripplanner.transit.service.TransitService;

/**
 * Convenience fetcher for RAPTOR transit data.
 */
public class RaptorTransitDataFetcher {

  private final TransitService transitService;
  private final LocalDate serviceDate;

  RaptorTransitDataFetcher(TransitService transitService, LocalDate serviceDate) {
    this.transitService = transitService;
    this.serviceDate = serviceDate;
  }

  /**
   * Get the patterns for the given service date, extract their ids, converts to string, sort
   * them alphabetically and add the real-time states of their trip times.
   */
  public List<String> summarizePatterns() {
    return list().stream().map(RaptorTransitDataFetcher::summarise).toList();
  }

  private Collection<TripPatternForDate> list() {
    return transitService
      .getRealtimeRaptorTransitData()
      .getTripPatternsForRunningDate(serviceDate)
      .stream()
      .sorted(Comparator.comparing(t -> t.getTripPattern().getPattern().getId().toString()))
      .toList();
  }

  private static String summarise(TripPatternForDate t) {
    var states = t
      .tripTimes()
      .stream()
      .map(tt -> tt.getRealTimeState().toString())
      .collect(Collectors.joining(","));

    var id = t.getTripPattern().getPattern().getId();
    return String.format("%s[%s]", id, states);
  }
}
