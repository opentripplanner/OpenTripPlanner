package org.opentripplanner.transit.api.request;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.transit.api.model.FilterValues;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class TripRequestBuilder {

  private FilterValues<FeedScopedId> includedAgencies = FilterValues.ofEmptyIsEverything(
    "includedAgencies",
    List.of()
  );
  private FilterValues<FeedScopedId> includedRoutes = FilterValues.ofEmptyIsEverything(
    "includedRoutes",
    List.of()
  );

  private FilterValues<FeedScopedId> excludedAgencies = FilterValues.ofEmptyIsEverything(
    "excludedAgencies",
    List.of()
  );
  private FilterValues<FeedScopedId> excludedRoutes = FilterValues.ofEmptyIsEverything(
    "excludedRoutes",
    List.of()
  );

  private FilterValues<String> netexInternalPlanningCodes = FilterValues.ofEmptyIsEverything(
    "netexInternalPlanningCodes",
    List.of()
  );
  private FilterValues<LocalDate> serviceDates = FilterValues.ofEmptyIsEverything(
    "serviceDates",
    List.of()
  );

  TripRequestBuilder() {}

  public TripRequestBuilder withIncludedAgencies(Collection<FeedScopedId> agencies) {
    this.includedAgencies = FilterValues.ofEmptyIsNothing("includedAgencies", agencies);
    return this;
  }

  public TripRequestBuilder withIncludedRoutes(Collection<FeedScopedId> routes) {
    this.includedRoutes = FilterValues.ofEmptyIsNothing("includedRoutes", routes);
    return this;
  }

  public TripRequestBuilder withExcludedAgencies(Collection<FeedScopedId> agencies) {
    this.excludedAgencies = FilterValues.ofEmptyIsEverything("excludedAgencies", agencies);
    return this;
  }

  public TripRequestBuilder withExcludedRoutes(Collection<FeedScopedId> routes) {
    this.excludedRoutes = FilterValues.ofEmptyIsEverything("excludedRoutes", routes);
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
    return new TripRequest(
      includedAgencies,
      includedRoutes,
      excludedAgencies,
      excludedRoutes,
      netexInternalPlanningCodes,
      serviceDates
    );
  }
}
