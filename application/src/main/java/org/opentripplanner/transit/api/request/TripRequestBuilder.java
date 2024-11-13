package org.opentripplanner.transit.api.request;

import java.time.LocalDate;
import org.opentripplanner.transit.api.model.CriteriaCollection;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class TripRequestBuilder {

  private CriteriaCollection<FeedScopedId> agencies;
  private CriteriaCollection<FeedScopedId> routes;
  private CriteriaCollection<String> netexInternalPlanningCodes;
  private CriteriaCollection<LocalDate> serviceDates;

  protected TripRequestBuilder() {}

  public TripRequestBuilder withAgencies(CriteriaCollection<FeedScopedId> agencies) {
    this.agencies = agencies;
    return this;
  }

  public TripRequestBuilder withRoutes(CriteriaCollection<FeedScopedId> routes) {
    this.routes = routes;
    return this;
  }

  public TripRequestBuilder withNetexInternalPlanningCodes(
    CriteriaCollection<String> netexInternalPlanningCodes
  ) {
    this.netexInternalPlanningCodes = netexInternalPlanningCodes;
    return this;
  }

  public TripRequestBuilder withServiceDates(CriteriaCollection<LocalDate> serviceDates) {
    this.serviceDates = serviceDates;
    return this;
  }

  public TripRequest build() {
    return new TripRequest(agencies, routes, netexInternalPlanningCodes, serviceDates);
  }
}
