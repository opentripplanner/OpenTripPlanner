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
import org.opentripplanner.core.model.time.LocalDateRange;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.transit.api.model.FilterValues;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.filter.selector.FilterRequest;
import org.opentripplanner.transit.model.filter.transit.TripTimeOnDateSelectRequest;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.ArrivalDeparture;

public class TripTimeOnDateRequest {

  private final Set<StopLocation> stopLocations;

  @Nullable
  private final Instant time;

  private final List<LocalDateRange> serviceDateRanges;
  private final CancellationPolicy cancellationPolicy;
  private final FilterValues<FeedScopedId> includeAgencies;
  private final FilterValues<FeedScopedId> includeRoutes;
  private final FilterValues<FeedScopedId> excludeAgencies;
  private final FilterValues<FeedScopedId> excludeRoutes;
  private final FilterValues<TransitMode> includeModes;
  private final FilterValues<TransitMode> excludeModes;
  private final List<FilterRequest<TripTimeOnDateSelectRequest>> transitFilters;
  private final Duration timeWindow;
  private final ArrivalDeparture arrivalDeparture;
  private final int numberOfDepartures;
  private final Comparator<TripTimeOnDate> sortOrder;

  public TripTimeOnDateRequest(
    Collection<StopLocation> stopLocations,
    @Nullable Instant time,
    List<LocalDateRange> serviceDateRanges,
    Duration timeWindow,
    ArrivalDeparture arrivalDeparture,
    int numberOfDepartures,
    Comparator<TripTimeOnDate> sortOrder,
    CancellationPolicy cancellationPolicy,
    FilterValues<FeedScopedId> includeAgencies,
    FilterValues<FeedScopedId> includeRoutes,
    FilterValues<FeedScopedId> excludeAgencies,
    FilterValues<FeedScopedId> excludeRoutes,
    FilterValues<TransitMode> includeModes,
    FilterValues<TransitMode> excludeModes,
    List<FilterRequest<TripTimeOnDateSelectRequest>> transitFilters
  ) {
    this.stopLocations = Set.copyOf(stopLocations);
    this.serviceDateRanges = List.copyOf(serviceDateRanges);
    this.time = time;
    this.timeWindow = timeWindow;
    this.arrivalDeparture = arrivalDeparture;
    this.numberOfDepartures = numberOfDepartures;
    this.sortOrder = Objects.requireNonNull(sortOrder);
    this.cancellationPolicy = Objects.requireNonNull(cancellationPolicy);
    this.includeAgencies = includeAgencies;
    this.includeRoutes = includeRoutes;
    this.excludeAgencies = excludeAgencies;
    this.excludeRoutes = excludeRoutes;
    this.includeModes = includeModes;
    this.excludeModes = excludeModes;
    this.transitFilters = List.copyOf(transitFilters);

    boolean usesTimeWindow = time != null;
    boolean usesServiceDateRanges = !this.serviceDateRanges.isEmpty();
    if (usesTimeWindow == usesServiceDateRanges) {
      throw new IllegalArgumentException(
        "Either 'time' or 'serviceDateRanges' must be set as the time limitation, but not both."
      );
    }
  }

  public static TripTimeOnDateRequestBuilder of(Collection<StopLocation> stopLocations) {
    return new TripTimeOnDateRequestBuilder(stopLocations);
  }

  public Set<StopLocation> stopLocations() {
    return stopLocations;
  }

  /**
   * The start time for a time-window based search. Returns {@code null} when the search is limited
   * by {@link #serviceDateRanges()} instead.
   */
  @Nullable
  public Instant time() {
    return time;
  }

  /**
   * The service date ranges to limit the search by, using each trip time's service date. When this
   * is non-empty the search ignores {@link #time()} and {@link #timeWindow()}.
   */
  public List<LocalDateRange> serviceDateRanges() {
    return serviceDateRanges;
  }

  public CancellationPolicy cancellationPolicy() {
    return cancellationPolicy;
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

  public List<FilterRequest<TripTimeOnDateSelectRequest>> transitFilters() {
    return transitFilters;
  }
}
