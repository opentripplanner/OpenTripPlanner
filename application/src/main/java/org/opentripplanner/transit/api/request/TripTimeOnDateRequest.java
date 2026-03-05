package org.opentripplanner.transit.api.request;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.transit.api.model.FilterValues;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.filter.transit.TripTimeOnDateFilterRequest;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.ArrivalDeparture;

public class TripTimeOnDateRequest {

  private final Set<StopLocation> stopLocations;
  private final Instant time;
  private final boolean includeCancelledTrips;
  private final FilterValues<FeedScopedId> includeAgencies;
  private final FilterValues<FeedScopedId> includeRoutes;
  private final FilterValues<FeedScopedId> excludeAgencies;
  private final FilterValues<FeedScopedId> excludeRoutes;
  private final FilterValues<TransitMode> includeModes;
  private final FilterValues<TransitMode> excludeModes;
  private final List<TripTimeOnDateFilterRequest> transitFilters;
  private final Duration timeWindow;
  private final ArrivalDeparture arrivalDeparture;
  private final int numberOfDepartures;
  private final Comparator<TripTimeOnDate> sortOrder;
  private final Integer departuresPerLineAndDestinationDisplay;

  public TripTimeOnDateRequest(
    Collection<StopLocation> stopLocations,
    Instant time,
    Duration timeWindow,
    ArrivalDeparture arrivalDeparture,
    int numberOfDepartures,
    Comparator<TripTimeOnDate> sortOrder,
    boolean includeCancelledTrips,
    FilterValues<FeedScopedId> includeAgencies,
    FilterValues<FeedScopedId> includeRoutes,
    FilterValues<FeedScopedId> excludeAgencies,
    FilterValues<FeedScopedId> excludeRoutes,
    FilterValues<TransitMode> includeModes,
    FilterValues<TransitMode> excludeModes,
    @Nullable Integer departuresPerLineAndDestinationDisplay,
    List<TripTimeOnDateFilterRequest> transitFilters
  ) {
    this.stopLocations = Set.copyOf(stopLocations);
    this.time = Objects.requireNonNull(time);
    this.timeWindow = timeWindow;
    this.arrivalDeparture = arrivalDeparture;
    this.numberOfDepartures = numberOfDepartures;
    this.sortOrder = Objects.requireNonNull(sortOrder);
    this.includeCancelledTrips = includeCancelledTrips;
    this.includeAgencies = includeAgencies;
    this.includeRoutes = includeRoutes;
    this.excludeAgencies = excludeAgencies;
    this.excludeRoutes = excludeRoutes;
    this.includeModes = includeModes;
    this.excludeModes = excludeModes;
    this.departuresPerLineAndDestinationDisplay = departuresPerLineAndDestinationDisplay;
    this.transitFilters = List.copyOf(transitFilters);
  }

  public static TripTimeOnDateRequestBuilder of(Collection<StopLocation> stopLocations) {
    return new TripTimeOnDateRequestBuilder(stopLocations);
  }

  public Set<StopLocation> stopLocations() {
    return stopLocations;
  }

  public Instant time() {
    return time;
  }

  public boolean includeCancelledTrips() {
    return includeCancelledTrips;
  }

  public FilterValues<FeedScopedId> includeAgencies() {
    return includeAgencies;
  }

  public FilterValues<FeedScopedId> includeRoutes() {
    return includeRoutes;
  }

  public FilterValues<FeedScopedId> excludeAgencies() {
    return excludeAgencies;
  }

  public FilterValues<FeedScopedId> excludeRoutes() {
    return excludeRoutes;
  }

  public FilterValues<TransitMode> includeModes() {
    return includeModes;
  }

  public FilterValues<TransitMode> excludeModes() {
    return excludeModes;
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

  @Nullable
  public Integer departuresPerLineAndDestinationDisplay() {
    return departuresPerLineAndDestinationDisplay;
  }

  public List<TripTimeOnDateFilterRequest> transitFilters() {
    return transitFilters;
  }
}
