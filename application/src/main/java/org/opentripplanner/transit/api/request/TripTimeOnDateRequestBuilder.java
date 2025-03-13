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

  private final Collection<StopLocation> stopLocations;
  private FilterValues<FeedScopedId> includedAgencies = FilterValues.ofEmptyIsEverything(
    "agencies",
    List.of()
  );
  private FilterValues<FeedScopedId> includedRoutes = FilterValues.ofEmptyIsEverything(
    "routes",
    List.of()
  );
  private FilterValues<FeedScopedId> excludedAgencies = FilterValues.ofEmptyIsEverything(
    "agencies",
    List.of()
  );
  private FilterValues<FeedScopedId> excludedRoutes = FilterValues.ofEmptyIsEverything(
    "routes",
    List.of()
  );
  private FilterValues<TransitMode> includedModes = FilterValues.ofEmptyIsEverything(
    "modes",
    List.of()
  );
  private FilterValues<TransitMode> excludedModes = FilterValues.ofEmptyIsEverything(
    "modes",
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

  public TripTimeOnDateRequestBuilder withIncludedAgencies(FilterValues<FeedScopedId> agencies) {
    this.includedAgencies = agencies;
    return this;
  }

  public TripTimeOnDateRequestBuilder withIncludedRoutes(FilterValues<FeedScopedId> routes) {
    this.includedRoutes = routes;
    return this;
  }

  public TripTimeOnDateRequestBuilder withExcludedAgencies(FilterValues<FeedScopedId> agencies) {
    this.excludedAgencies = agencies;
    return this;
  }

  public TripTimeOnDateRequestBuilder withExcludedRoutes(FilterValues<FeedScopedId> routes) {
    this.excludedRoutes = routes;
    return this;
  }

  public TripTimeOnDateRequestBuilder withIncludedModes(FilterValues<TransitMode> modes) {
    this.includedModes = modes;
    return this;
  }

  public TripTimeOnDateRequestBuilder withExcludedModes(FilterValues<TransitMode> modes) {
    this.excludedModes = modes;
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
