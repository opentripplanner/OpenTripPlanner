package org.opentripplanner.transit.api.request;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.transit.api.model.FilterValues;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.ArrivalDeparture;

public class TripTimeOnDateRequestBuilder {

  private static final String INCLUDE_AGENCIES = "includeAgencies";
  private static final String INCLUDE_ROUTES = "includeRoutes";
  private static final String EXCLUDE_AGENCIES = "excludeAgencies";
  private static final String INCLUDE_MODES = "includeModes";
  private static final String EXCLUDE_ROUTES = "excludeRoutes";
  private static final String EXCLUDE_MODES = "excludeModes";
  private final Collection<StopLocation> stopLocations;
  private FilterValues<FeedScopedId> includeAgencies = FilterValues.ofNullIsEverything(
    INCLUDE_AGENCIES,
    null
  );
  private FilterValues<FeedScopedId> includeRoutes = FilterValues.ofNullIsEverything(
    INCLUDE_ROUTES,
    null
  );
  private FilterValues<FeedScopedId> excludeAgencies = FilterValues.ofEmptyIsEverything(
    EXCLUDE_AGENCIES,
    List.of()
  );
  private FilterValues<FeedScopedId> excludeRoutes = FilterValues.ofEmptyIsEverything(
    EXCLUDE_ROUTES,
    List.of()
  );
  private FilterValues<TransitMode> includeModes = FilterValues.ofNullIsEverything(
    INCLUDE_MODES,
    null
  );
  private FilterValues<TransitMode> excludeModes = FilterValues.ofEmptyIsEverything(
    EXCLUDE_MODES,
    List.of()
  );
  private Duration timeWindow = Duration.ofHours(2);
  private ArrivalDeparture arrivalDeparture = ArrivalDeparture.BOTH;
  private int numberOfDepartures = 10;
  private Instant time;
  private Comparator<TripTimeOnDate> sortOrder = TripTimeOnDate.compareByDeparture();

  TripTimeOnDateRequestBuilder(Collection<StopLocation> timesAtStops) {
    this.stopLocations = timesAtStops;
  }

  public TripTimeOnDateRequestBuilder withTime(Instant time) {
    this.time = time;
    return this;
  }

  public TripTimeOnDateRequestBuilder withIncludeAgencies(
    @Nullable Collection<FeedScopedId> agencies
  ) {
    this.includeAgencies = FilterValues.ofNullIsEverything(INCLUDE_AGENCIES, agencies);
    return this;
  }

  public TripTimeOnDateRequestBuilder withIncludeRoutes(@Nullable Collection<FeedScopedId> routes) {
    this.includeRoutes = FilterValues.ofNullIsEverything(INCLUDE_ROUTES, routes);
    return this;
  }

  public TripTimeOnDateRequestBuilder withExcludeAgencies(Collection<FeedScopedId> agencies) {
    this.excludeAgencies = FilterValues.ofEmptyIsEverything(EXCLUDE_AGENCIES, agencies);
    return this;
  }

  public TripTimeOnDateRequestBuilder withExcludeRoutes(Collection<FeedScopedId> routes) {
    this.excludeRoutes = FilterValues.ofEmptyIsEverything(EXCLUDE_ROUTES, routes);
    return this;
  }

  public TripTimeOnDateRequestBuilder withIncludeModes(@Nullable Collection<TransitMode> modes) {
    this.includeModes = FilterValues.ofNullIsEverything(INCLUDE_MODES, modes);
    return this;
  }

  public TripTimeOnDateRequestBuilder withExcludeModes(Collection<TransitMode> modes) {
    this.excludeModes = FilterValues.ofEmptyIsEverything(EXCLUDE_MODES, modes);
    return this;
  }

  public TripTimeOnDateRequestBuilder withArrivalDeparture(ArrivalDeparture arrivalDeparture) {
    this.arrivalDeparture = arrivalDeparture;
    return this;
  }

  public TripTimeOnDateRequestBuilder withTimeWindow(Duration timeWindow) {
    this.timeWindow = timeWindow;
    return this;
  }

  public TripTimeOnDateRequestBuilder withNumberOfDepartures(int numberOfDepartures) {
    this.numberOfDepartures = numberOfDepartures;
    return this;
  }

  public TripTimeOnDateRequestBuilder withSortOrder(Comparator<TripTimeOnDate> sortOrder) {
    this.sortOrder = sortOrder;
    return this;
  }

  public TripTimeOnDateRequest build() {
    return new TripTimeOnDateRequest(
      stopLocations,
      time,
      timeWindow,
      arrivalDeparture,
      numberOfDepartures,
      sortOrder,
      includeAgencies,
      includeRoutes,
      excludeAgencies,
      excludeRoutes,
      includeModes,
      excludeModes
    );
  }
}
