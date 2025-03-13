package org.opentripplanner.transit.api.request;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.transit.api.model.FilterValues;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.ArrivalDeparture;

public class TripTimeOnDateRequestBuilder {

  private static final String INCLUDED_AGENCIES = "includeAgencies";
  private static final String INCLUDED_ROUTES = "includeRoutes";
  private static final String EXCLUDED_AGENCIES = "excludeAgencies";
  private static final String INCLUDED_MODES = "includeModes";
  private static final String EXCLUDED_ROUTES = "excludeRoutes";
  private static final String EXCLUDED_MODES = "excludeModes";
  private final Collection<StopLocation> stopLocations;
  private FilterValues<FeedScopedId> includedAgencies = FilterValues.ofEmptyIsEverything(
    INCLUDED_AGENCIES,
    List.of()
  );
  private FilterValues<FeedScopedId> includedRoutes = FilterValues.ofEmptyIsEverything(
    INCLUDED_ROUTES,
    List.of()
  );
  private FilterValues<FeedScopedId> excludedAgencies = FilterValues.ofEmptyIsEverything(
    EXCLUDED_AGENCIES,
    List.of()
  );
  private FilterValues<FeedScopedId> excludedRoutes = FilterValues.ofEmptyIsEverything(
    EXCLUDED_ROUTES,
    List.of()
  );
  private FilterValues<TransitMode> includedModes = FilterValues.ofEmptyIsEverything(
    INCLUDED_MODES,
    List.of()
  );
  private FilterValues<TransitMode> excludedModes = FilterValues.ofEmptyIsEverything(
    EXCLUDED_MODES,
    List.of()
  );
  private Duration timeWindow = Duration.ofHours(2);
  private ArrivalDeparture arrivalDeparture = ArrivalDeparture.BOTH;
  private int numberOfDepartures = 10;
  private Instant time;
  private Comparator<TripTimeOnDate> sortOrder = TripTimeOnDate.compareByRealtimeDeparture();

  TripTimeOnDateRequestBuilder(Collection<StopLocation> timesAtStops) {
    this.stopLocations = timesAtStops;
  }

  public TripTimeOnDateRequestBuilder withTime(Instant time) {
    this.time = time;
    return this;
  }

  public TripTimeOnDateRequestBuilder withIncludedAgencies(Collection<FeedScopedId> agencies) {
    this.includedAgencies = FilterValues.ofEmptyIsEverything(INCLUDED_AGENCIES, agencies);
    return this;
  }

  public TripTimeOnDateRequestBuilder withIncludedRoutes(Collection<FeedScopedId> routes) {
    this.includedRoutes = FilterValues.ofEmptyIsEverything(INCLUDED_ROUTES, routes);
    return this;
  }

  public TripTimeOnDateRequestBuilder withExcludedAgencies(Collection<FeedScopedId> agencies) {
    this.excludedAgencies = FilterValues.ofEmptyIsEverything(EXCLUDED_AGENCIES, agencies);
    return this;
  }

  public TripTimeOnDateRequestBuilder withExcludedRoutes(Collection<FeedScopedId> routes) {
    this.excludedRoutes = FilterValues.ofEmptyIsEverything(EXCLUDED_ROUTES, routes);
    return this;
  }

  public TripTimeOnDateRequestBuilder withIncludedModes(Collection<TransitMode> modes) {
    this.includedModes = FilterValues.ofEmptyIsEverything(INCLUDED_MODES, modes);
    return this;
  }

  public TripTimeOnDateRequestBuilder withExcludedModes(Collection<TransitMode> modes) {
    this.excludedModes = FilterValues.ofEmptyIsEverything(EXCLUDED_MODES, modes);
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
      includedAgencies,
      includedRoutes,
      excludedAgencies,
      excludedRoutes,
      includedModes,
      excludedModes
    );
  }
}
