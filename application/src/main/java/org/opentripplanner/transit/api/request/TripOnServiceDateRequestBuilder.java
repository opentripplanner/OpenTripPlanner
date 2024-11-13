package org.opentripplanner.transit.api.request;

import java.time.LocalDate;
import org.opentripplanner.transit.api.model.CriteriaCollection;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.timetable.TripAlteration;

public class TripOnServiceDateRequestBuilder {

  private CriteriaCollection<FeedScopedId> agencies;
  private CriteriaCollection<FeedScopedId> routes;
  private CriteriaCollection<FeedScopedId> serviceJourneys;
  private CriteriaCollection<FeedScopedId> replacementFor;
  private CriteriaCollection<String> netexInternalPlanningCodes;
  private CriteriaCollection<TripAlteration> alterations;
  private CriteriaCollection<LocalDate> serviceDates;

  protected TripOnServiceDateRequestBuilder() {}

  public TripOnServiceDateRequestBuilder withServiceDates(
    CriteriaCollection<LocalDate> serviceDates
  ) {
    this.serviceDates = serviceDates;
    return this;
  }

  public TripOnServiceDateRequestBuilder withAgencies(CriteriaCollection<FeedScopedId> agencies) {
    this.agencies = agencies;
    return this;
  }

  public TripOnServiceDateRequestBuilder withRoutes(CriteriaCollection<FeedScopedId> routes) {
    this.routes = routes;
    return this;
  }

  public TripOnServiceDateRequestBuilder withServiceJourneys(
    CriteriaCollection<FeedScopedId> serviceJourneys
  ) {
    this.serviceJourneys = serviceJourneys;
    return this;
  }

  public TripOnServiceDateRequestBuilder withReplacementFor(
    CriteriaCollection<FeedScopedId> replacementFor
  ) {
    this.replacementFor = replacementFor;
    return this;
  }

  public TripOnServiceDateRequestBuilder withNetexInternalPlanningCodes(
    CriteriaCollection<String> netexInternalPlanningCodes
  ) {
    this.netexInternalPlanningCodes = netexInternalPlanningCodes;
    return this;
  }

  public TripOnServiceDateRequestBuilder withAlterations(
    CriteriaCollection<TripAlteration> alterations
  ) {
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
