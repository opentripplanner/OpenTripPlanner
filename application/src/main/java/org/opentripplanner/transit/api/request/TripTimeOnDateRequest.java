package org.opentripplanner.transit.api.request;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.opentripplanner.routing.stoptimes.ArrivalDeparture;
import org.opentripplanner.transit.api.model.FilterValues;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.StopLocation;

public class TripTimeOnDateRequest {

  private final Set<StopLocation> stopLocations;
  private final Instant time;
  private final FilterValues<FeedScopedId> selectedAgencies;
  private final FilterValues<FeedScopedId> selectedRoutes;
  private final FilterValues<TransitMode> modes;
  private final Duration timeWindow;
  private final ArrivalDeparture arrivalDeparture;
  private final int numberOfDepartures;

  TripTimeOnDateRequest(
    Collection<StopLocation> stopLocations,
    Instant time,
    Duration timeWindow,
    ArrivalDeparture arrivalDeparture,
    int numberOfDepartures,
    FilterValues<FeedScopedId> selectedAgencies,
    FilterValues<FeedScopedId> selectedRoutes,
    FilterValues<TransitMode> modes
  ) {
    this.stopLocations = Set.copyOf(stopLocations);
    this.time = Objects.requireNonNull(time);
    this.timeWindow = timeWindow;
    this.arrivalDeparture = arrivalDeparture;
    this.numberOfDepartures = numberOfDepartures;
    this.selectedAgencies = selectedAgencies;
    this.selectedRoutes = selectedRoutes;
    this.modes = modes;
  }

  public static TripTimeOnDateRequestBuilder of(List<StopLocation> stopLocations) {
    return new TripTimeOnDateRequestBuilder(stopLocations);
  }

  public Set<StopLocation> stopLocations() {
    return stopLocations;
  }

  public Instant time() {
    return time;
  }

  public FilterValues<FeedScopedId> agencies() {
    return selectedAgencies;
  }

  public FilterValues<FeedScopedId> routes() {
    return selectedRoutes;
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

  public int numberOfDepartures() {
    return numberOfDepartures;
  }
}
