package org.opentripplanner.transit.api.request;

import java.time.LocalDate;
import java.util.List;
import org.opentripplanner.transit.api.model.FilterValues;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class TripRequestBuilder {

  private FilterValues<FeedScopedId> includedAgencies = FilterValues.ofEmptyIsEverything(
    "selectedAgencies",
    List.of()
  );
  private FilterValues<FeedScopedId> includedRoutes = FilterValues.ofEmptyIsEverything("selectedRoutes", List.of());

  private FilterValues<FeedScopedId> excludedAgencies = FilterValues.ofEmptyIsEverything(
    "excludedAgencies",
    List.of()
  );
  private FilterValues<FeedScopedId> excludedRoutes = FilterValues.ofEmptyIsEverything("excludedRoutes", List.of());


  private FilterValues<String> netexInternalPlanningCodes = FilterValues.ofEmptyIsEverything(
    "netexInternalPlanningCodes",
    List.of()
  );
  private FilterValues<LocalDate> serviceDates = FilterValues.ofEmptyIsEverything(
    "serviceDates",
    List.of()
  );

  TripRequestBuilder() {}

  public TripRequestBuilder withIncludedAgencies(FilterValues<FeedScopedId> agencies) {
    this.includedAgencies = agencies;
    return this;
  }

  public TripRequestBuilder withIncludedRoutes(FilterValues<FeedScopedId> routes) {
    this.includedRoutes = routes;
    return this;
  }

  public TripRequestBuilder withExcludedAgencies(FilterValues<FeedScopedId> agencies) {
    this.excludedAgencies=  agencies;
    return this;
  }

  public TripRequestBuilder withExcludedRoutes(FilterValues<FeedScopedId> routes) {
    this.excludedRoutes= routes;
    return this;
  }

  public TripRequestBuilder withNetexInternalPlanningCodes(
    FilterValues<String> netexInternalPlanningCodes
  ) {
    this.netexInternalPlanningCodes = netexInternalPlanningCodes;
    return this;
  }

  public TripRequestBuilder withServiceDates(FilterValues<LocalDate> serviceDates) {
    this.serviceDates = serviceDates;
    return this;
  }

  public TripRequest build() {
    return new TripRequest(includedAgencies, includedRoutes, excludedAgencies, excludedRoutes, netexInternalPlanningCodes, serviceDates);
  }
}
