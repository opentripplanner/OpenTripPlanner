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

  private static final String INCLUDE_AGENCIES = "includeAgencies";
  private static final String INCLUDE_ROUTES = "includeRoutes";
  private static final String EXCLUDE_AGENCIES = "excludeAgencies";
  private static final String INCLUDE_MODES = "includeModes";
  private static final String EXCLUDE_ROUTES = "excludeRoutes";
  private static final String EXCLUDE_MODES = "excludeModes";
  private final Collection<StopLocation> stopLocations;
  private FilterValues<FeedScopedId> INCLUDEAgencies = FilterValues.ofEmptyIsEverything(
    INCLUDE_AGENCIES,
    List.of()
  );
  private FilterValues<FeedScopedId> INCLUDERoutes = FilterValues.ofEmptyIsEverything(
    INCLUDE_ROUTES,
    List.of()
  );
  private FilterValues<FeedScopedId> EXCLUDEAgencies = FilterValues.ofEmptyIsEverything(
    EXCLUDE_AGENCIES,
    List.of()
  );
  private FilterValues<FeedScopedId> EXCLUDERoutes = FilterValues.ofEmptyIsEverything(
    EXCLUDE_ROUTES,
    List.of()
  );
  private FilterValues<TransitMode> INCLUDEModes = FilterValues.ofEmptyIsEverything(
    INCLUDE_MODES,
    List.of()
  );
  private FilterValues<TransitMode> EXCLUDEModes = FilterValues.ofEmptyIsEverything(
    EXCLUDE_MODES,
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

  public TripTimeOnDateRequestBuilder withIncludeAgencies(Collection<FeedScopedId> agencies) {
    this.INCLUDEAgencies = FilterValues.ofEmptyIsEverything(INCLUDE_AGENCIES, agencies);
    return this;
  }

  public TripTimeOnDateRequestBuilder withIncludeRoutes(Collection<FeedScopedId> routes) {
    this.INCLUDERoutes = FilterValues.ofEmptyIsEverything(INCLUDE_ROUTES, routes);
    return this;
  }

  public TripTimeOnDateRequestBuilder withExcludeAgencies(Collection<FeedScopedId> agencies) {
    this.EXCLUDEAgencies = FilterValues.ofEmptyIsEverything(EXCLUDE_AGENCIES, agencies);
    return this;
  }

  public TripTimeOnDateRequestBuilder withExcludeRoutes(Collection<FeedScopedId> routes) {
    this.EXCLUDERoutes = FilterValues.ofEmptyIsEverything(EXCLUDE_ROUTES, routes);
    return this;
  }

  public TripTimeOnDateRequestBuilder withIncludeModes(Collection<TransitMode> modes) {
    this.INCLUDEModes = FilterValues.ofEmptyIsEverything(INCLUDE_MODES, modes);
    return this;
  }

  public TripTimeOnDateRequestBuilder withExcludeModes(Collection<TransitMode> modes) {
    this.EXCLUDEModes = FilterValues.ofEmptyIsEverything(EXCLUDE_MODES, modes);
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
      INCLUDEAgencies,
      INCLUDERoutes,
      EXCLUDEAgencies,
      EXCLUDERoutes,
      INCLUDEModes,
      EXCLUDEModes
    );
  }
}
