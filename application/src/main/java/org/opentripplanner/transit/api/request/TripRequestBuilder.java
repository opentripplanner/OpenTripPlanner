package org.opentripplanner.transit.api.request;

import java.time.LocalDate;
import java.util.List;
import org.opentripplanner.transit.api.model.FilterValues;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class TripRequestBuilder {

  private FilterValues<FeedScopedId> agencies = FilterValues.ofEmptyIsEverything(
    "agencies",
    List.of()
  );
  private FilterValues<FeedScopedId> routes = FilterValues.ofEmptyIsEverything("routes", List.of());
  private FilterValues<String> netexInternalPlanningCodes = FilterValues.ofEmptyIsEverything(
    "netexInternalPlanningCodes",
    List.of()
  );
  private FilterValues<LocalDate> serviceDates = FilterValues.ofEmptyIsEverything(
    "serviceDates",
    List.of()
  );

  TripRequestBuilder() {}

  public TripRequestBuilder withAgencies(FilterValues<FeedScopedId> agencies) {
    this.agencies = agencies;
    return this;
  }

  public TripRequestBuilder withRoutes(FilterValues<FeedScopedId> routes) {
    this.routes = routes;
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
    return new TripRequest(agencies, routes, netexInternalPlanningCodes, serviceDates);
  }
}
