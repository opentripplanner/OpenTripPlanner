package org.opentripplanner.transit.api.request;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.transit.api.model.FilterValues;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.ArrivalDeparture;

public class TripTimeOnDateRequest {

  private final Set<StopLocation> stopLocations;
  private final Instant time;
  private final FilterValues<FeedScopedId> selectedAgencies;
  private final FilterValues<FeedScopedId> selectedRoutes;
  private final FilterValues<FeedScopedId> excludedAgencies;
  private final FilterValues<FeedScopedId> excludedRoutes;
  private final FilterValues<TransitMode> includedModes;
  private final FilterValues<TransitMode> excludedModes;
  private final Duration timeWindow;
  private final ArrivalDeparture arrivalDeparture;
  private final int numberOfDepartures;
  private final Comparator<TripTimeOnDate> sortOrder;

  TripTimeOnDateRequest(
    Collection<StopLocation> stopLocations,
    Instant time,
    Duration timeWindow,
    ArrivalDeparture arrivalDeparture,
    int numberOfDepartures,
    Comparator<TripTimeOnDate> sortOrder,
    FilterValues<FeedScopedId> selectedAgencies,
    FilterValues<FeedScopedId> selectedRoutes,
    FilterValues<FeedScopedId> excludedAgencies,
    FilterValues<FeedScopedId> excludedRoutes,
    FilterValues<TransitMode> includedModes,
    FilterValues<TransitMode> excludedModes
  ) {
    this.stopLocations = Set.copyOf(stopLocations);
    this.time = Objects.requireNonNull(time);
    this.timeWindow = timeWindow;
    this.arrivalDeparture = arrivalDeparture;
    this.numberOfDepartures = numberOfDepartures;
    this.sortOrder = Objects.requireNonNull(sortOrder);
    this.selectedAgencies = selectedAgencies;
    this.selectedRoutes = selectedRoutes;
    this.excludedAgencies = excludedAgencies;
    this.excludedRoutes = excludedRoutes;
    this.includedModes = includedModes;
    this.excludedModes = excludedModes;
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

  public FilterValues<FeedScopedId> selectedAgencies() {
    return selectedAgencies;
  }

  public FilterValues<FeedScopedId> selectedRoutes() {
    return selectedRoutes;
  }

  public FilterValues<FeedScopedId> excludedAgencies() {
    return excludedAgencies;
  }

  public FilterValues<FeedScopedId> excludedRoutes() {
    return excludedRoutes;
  }

  public FilterValues<TransitMode> includedModes() {
    return includedModes;
  }

  public FilterValues<TransitMode> excludedModes() {
    return excludedModes;
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

  public Comparator<TripTimeOnDate> sortOrder() {
    return sortOrder;
  }
}
