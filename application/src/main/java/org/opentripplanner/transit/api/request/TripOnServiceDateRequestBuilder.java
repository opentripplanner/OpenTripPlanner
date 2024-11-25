package org.opentripplanner.transit.api.request;

import java.time.LocalDate;
import java.util.List;
import org.opentripplanner.transit.api.model.FilterValueCollection;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.timetable.TripAlteration;

public class TripOnServiceDateRequestBuilder {

  private FilterValueCollection<FeedScopedId> agencies = FilterValueCollection.ofEmptyIsEverything(
    List.of()
  );
  private FilterValueCollection<FeedScopedId> routes = FilterValueCollection.ofEmptyIsEverything(
    List.of()
  );
  private FilterValueCollection<FeedScopedId> serviceJourneys = FilterValueCollection.ofEmptyIsEverything(
    List.of()
  );
  private FilterValueCollection<FeedScopedId> replacementFor = FilterValueCollection.ofEmptyIsEverything(
    List.of()
  );
  private FilterValueCollection<String> netexInternalPlanningCodes = FilterValueCollection.ofEmptyIsEverything(
    List.of()
  );
  private FilterValueCollection<TripAlteration> alterations = FilterValueCollection.ofEmptyIsEverything(
    List.of()
  );
  private FilterValueCollection<LocalDate> serviceDates = FilterValueCollection.ofEmptyIsEverything(
    List.of()
  );

  protected TripOnServiceDateRequestBuilder() {}

  public TripOnServiceDateRequestBuilder withServiceDates(
    FilterValueCollection<LocalDate> serviceDates
  ) {
    this.serviceDates = serviceDates;
    return this;
  }

  public TripOnServiceDateRequestBuilder withAgencies(
    FilterValueCollection<FeedScopedId> agencies
  ) {
    this.agencies = agencies;
    return this;
  }

  public TripOnServiceDateRequestBuilder withRoutes(FilterValueCollection<FeedScopedId> routes) {
    this.routes = routes;
    return this;
  }

  public TripOnServiceDateRequestBuilder withServiceJourneys(
    FilterValueCollection<FeedScopedId> serviceJourneys
  ) {
    this.serviceJourneys = serviceJourneys;
    return this;
  }

  public TripOnServiceDateRequestBuilder withReplacementFor(
    FilterValueCollection<FeedScopedId> replacementFor
  ) {
    this.replacementFor = replacementFor;
    return this;
  }

  public TripOnServiceDateRequestBuilder withNetexInternalPlanningCodes(
    FilterValueCollection<String> netexInternalPlanningCodes
  ) {
    this.netexInternalPlanningCodes = netexInternalPlanningCodes;
    return this;
  }

  public TripOnServiceDateRequestBuilder withAlterations(
    FilterValueCollection<TripAlteration> alterations
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
