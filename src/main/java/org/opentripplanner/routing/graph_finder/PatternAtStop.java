package org.opentripplanner.routing.graph_finder;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.TripTimeShort;
import org.opentripplanner.routing.RoutingService;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

// TODO Is this the right package for this?
public class PatternAtStop {

  public String id;
  public Stop stop;
  public TripPattern pattern;

  public PatternAtStop(Stop stop, TripPattern pattern) {
    this.id = toId(stop, pattern);
    this.stop = stop;
    this.pattern = pattern;
  }

  private static String toId(Stop stop, TripPattern pattern) {
    Base64.Encoder encoder = Base64.getEncoder();
    return encoder.encodeToString(stop.getId().toString().getBytes(StandardCharsets.UTF_8)) + ";" +
        encoder.encodeToString(pattern.getId().toString().getBytes(StandardCharsets.UTF_8));
  }

  public List<TripTimeShort> getStoptimes(
      RoutingService routingService, long startTime, int timeRange, int numberOfDepartures,
      boolean omitNonPickups, boolean omitCanceled
  ) {
    return routingService.stopTimesForPatternAtStop(
        stop,
        pattern,
        startTime,
        timeRange,
        numberOfDepartures,
        omitNonPickups
    );
  }

  public static PatternAtStop fromId(RoutingService routingService, String id) {
    String[] parts = id.split(";", 2);
    Base64.Decoder decoder = Base64.getDecoder();
    FeedScopedId stopId = FeedScopedId.parseId(new String(decoder.decode(parts[0]), StandardCharsets.UTF_8));
    FeedScopedId patternId = FeedScopedId.parseId(new String(decoder.decode(parts[1]), StandardCharsets.UTF_8));
    return new PatternAtStop(routingService.getStopForId(stopId),
        routingService.getTripPatternForId(patternId)
    );
  }
}
