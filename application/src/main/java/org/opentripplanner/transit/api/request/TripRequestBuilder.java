package org.opentripplanner.transit.api.request;

import java.time.LocalDate;
import java.util.List;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class TripRequestBuilder {

  private List<FeedScopedId> agencies;
  private List<FeedScopedId> routes;
  private List<String> netexInternalPlanningCodes;
  private List<LocalDate> serviceDates;

  protected TripRequestBuilder() {}

  public TripRequestBuilder withAgencies(List<FeedScopedId> agencies) {
    this.agencies = agencies;
    return this;
  }

  public TripRequestBuilder withRoutes(List<FeedScopedId> routes) {
    this.routes = routes;
    return this;
  }

  public TripRequestBuilder withNetexInternalPlanningCodes(List<String> netexInternalPlanningCodes) {
    this.netexInternalPlanningCodes = netexInternalPlanningCodes;
    return this;
  }

  public TripRequestBuilder withServiceDates(List<LocalDate> serviceDates) {
    this.serviceDates = serviceDates;
    return this;
  }

  public TripRequest build() {
    return new TripRequest(agencies, routes, netexInternalPlanningCodes, serviceDates);
  }
}
