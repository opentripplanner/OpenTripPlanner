package org.opentripplanner.transit.api.request;

import java.time.LocalDate;
import java.util.List;
import org.opentripplanner.transit.api.model.FilterValueCollection;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class TripRequestBuilder {

  private FilterValueCollection<FeedScopedId> agencies = FilterValueCollection.ofEmptyIsEverything(
    List.of()
  );
  private FilterValueCollection<FeedScopedId> routes = FilterValueCollection.ofEmptyIsEverything(
    List.of()
  );
  private FilterValueCollection<String> netexInternalPlanningCodes = FilterValueCollection.ofEmptyIsEverything(
    List.of()
  );
  private FilterValueCollection<LocalDate> serviceDates = FilterValueCollection.ofEmptyIsEverything(
    List.of()
  );

  protected TripRequestBuilder() {}

  public TripRequestBuilder withAgencies(FilterValueCollection<FeedScopedId> agencies) {
    this.agencies = agencies;
    return this;
  }

  public TripRequestBuilder withRoutes(FilterValueCollection<FeedScopedId> routes) {
    this.routes = routes;
    return this;
  }

  public TripRequestBuilder withNetexInternalPlanningCodes(
    FilterValueCollection<String> netexInternalPlanningCodes
  ) {
    this.netexInternalPlanningCodes = netexInternalPlanningCodes;
    return this;
  }

  public TripRequestBuilder withServiceDates(FilterValueCollection<LocalDate> serviceDates) {
    this.serviceDates = serviceDates;
    return this;
  }

  public TripRequest build() {
    return new TripRequest(agencies, routes, netexInternalPlanningCodes, serviceDates);
  }
}
