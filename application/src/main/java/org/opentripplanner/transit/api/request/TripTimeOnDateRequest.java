package org.opentripplanner.transit.api.request;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.opentripplanner.routing.stoptimes.ArrivalDeparture;
import org.opentripplanner.transit.api.model.FilterValues;
import org.opentripplanner.transit.api.model.RequiredFilterValues;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.StopLocation;

public class TripTimeOnDateRequest {

  private final List<TimeAtStop> timesAtStops;
  private final FilterValues<FeedScopedId> agencies;
  private final FilterValues<FeedScopedId> routes;
  private final FilterValues<TransitMode> modes;
  private final Duration timeWindow;
  private final ArrivalDeparture arrivalDeparture;

  TripTimeOnDateRequest(
    List<TimeAtStop> timesAtStop,
    Duration timeWindow,
    ArrivalDeparture arrivalDeparture,
    FilterValues<TransitMode> modes,
    FilterValues<FeedScopedId> agencies,
    FilterValues<FeedScopedId> routes
  ) {
    this.timesAtStops = timesAtStop;
    this.agencies = agencies;
    this.routes = routes;
    this.modes = modes;
    this.timeWindow = timeWindow;
    this.arrivalDeparture = arrivalDeparture;
  }

  public static TripTimeOnDateRequestBuilder of(List<TimeAtStop> timesAtStops) {
    return new TripTimeOnDateRequestBuilder(timesAtStops);
  }

  public List<TimeAtStop> timesAtStop() {
    return timesAtStops;
  }

  public FilterValues<FeedScopedId> agencies() {
    return agencies;
  }

  public FilterValues<FeedScopedId> routes() {
    return routes;
  }

  public FilterValues<TransitMode> modes() {
    return modes;
  }

  public Duration timeWindow() {
    return timeWindow;
  }

  public ArrivalDeparture arrivalDeparture() {
    return arrivalDeparture;
  }

  public record TimeAtStop(StopLocation stop, Instant time) {}
}
