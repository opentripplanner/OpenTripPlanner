package org.opentripplanner.transit.api.request;

import java.time.LocalDate;
import java.util.List;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.timetable.TripAlteration;

public class TripOnServiceDateRequestBuilder {

  private List<FeedScopedId> agencies;
  private List<FeedScopedId> routes;
  private List<FeedScopedId> serviceJourneys;
  private List<FeedScopedId> replacementFor;
  private List<String> netexInternalPlanningCodes;
  private List<TripAlteration> alterations;
  private List<LocalDate> serviceDates;

  protected TripOnServiceDateRequestBuilder() {}

  public TripOnServiceDateRequestBuilder withServiceDates(List<LocalDate> serviceDates) {
    this.serviceDates = serviceDates;
    return this;
  }

  public TripOnServiceDateRequestBuilder withAgencies(List<FeedScopedId> agencies) {
    this.agencies = agencies;
    return this;
  }

  public TripOnServiceDateRequestBuilder withRoutes(List<FeedScopedId> routes) {
    this.routes = routes;
    return this;
  }

  public TripOnServiceDateRequestBuilder withServiceJourneys(List<FeedScopedId> serviceJourneys) {
    this.serviceJourneys = serviceJourneys;
    return this;
  }

  public TripOnServiceDateRequestBuilder withReplacementFor(List<FeedScopedId> replacementFor) {
    this.replacementFor = replacementFor;
    return this;
  }

  public TripOnServiceDateRequestBuilder withNetexInternalPlanningCodes(List<String> netexInternalPlanningCodes) {
    this.netexInternalPlanningCodes = netexInternalPlanningCodes;
    return this;
  }

  public TripOnServiceDateRequestBuilder withAlterations(List<TripAlteration> alterations) {
    this.alterations = alterations;
    return this;
  }

  public TripOnServiceDateRequest build() {
    return new TripOnServiceDateRequest(
      serviceDates,
      agencies,
      routes,
      serviceJourneys,
      replacementFor,
      netexInternalPlanningCodes,
      alterations
    );
  }
}
