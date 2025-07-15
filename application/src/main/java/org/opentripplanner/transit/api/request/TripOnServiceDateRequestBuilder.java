package org.opentripplanner.transit.api.request;

import java.time.LocalDate;
import java.util.List;
import org.opentripplanner.transit.api.model.FilterValues;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.timetable.TripAlteration;

public class TripOnServiceDateRequestBuilder {

  private FilterValues<FeedScopedId> agencies = FilterValues.ofEmptyIsEverything(
    "agencies",
    List.of()
  );
  private FilterValues<FeedScopedId> routes = FilterValues.ofEmptyIsEverything("routes", List.of());
  private FilterValues<FeedScopedId> serviceJourneys = FilterValues.ofEmptyIsEverything(
    "serviceJourneys",
    List.of()
  );
  private FilterValues<FeedScopedId> replacementFor = FilterValues.ofEmptyIsEverything(
    "replacementFor",
    List.of()
  );
  private FilterValues<String> netexInternalPlanningCodes = FilterValues.ofEmptyIsEverything(
    "netexInternalPlanningCodes",
    List.of()
  );
  private FilterValues<TripAlteration> alterations = FilterValues.ofEmptyIsEverything(
    "alterations",
    List.of()
  );
  private final FilterValues<LocalDate> serviceDates;

  TripOnServiceDateRequestBuilder(FilterValues<LocalDate> serviceDates) {
    this.serviceDates = serviceDates;
  }

  public TripOnServiceDateRequestBuilder withAgencies(FilterValues<FeedScopedId> agencies) {
    this.agencies = agencies;
    return this;
  }

  public TripOnServiceDateRequestBuilder withRoutes(FilterValues<FeedScopedId> routes) {
    this.routes = routes;
    return this;
  }

  public TripOnServiceDateRequestBuilder withServiceJourneys(
    FilterValues<FeedScopedId> serviceJourneys
  ) {
    this.serviceJourneys = serviceJourneys;
    return this;
  }

  public TripOnServiceDateRequestBuilder withReplacementFor(
    FilterValues<FeedScopedId> replacementFor
  ) {
    this.replacementFor = replacementFor;
    return this;
  }

  public TripOnServiceDateRequestBuilder withNetexInternalPlanningCodes(
    FilterValues<String> netexInternalPlanningCodes
  ) {
    this.netexInternalPlanningCodes = netexInternalPlanningCodes;
    return this;
  }

  public TripOnServiceDateRequestBuilder withAlterations(FilterValues<TripAlteration> alterations) {
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
