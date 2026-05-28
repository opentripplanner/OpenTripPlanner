package org.opentripplanner.transit.api.request;

import java.time.LocalDate;
import java.util.List;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.api.model.FilterValues;
import org.opentripplanner.transit.model.filter.selector.FilterRequest;
import org.opentripplanner.transit.model.filter.transit.TripOnServiceDateSelectRequest;
import org.opentripplanner.transit.model.timetable.TripAlteration;

public class TripOnServiceDateRequestBuilder {

  private FilterValues<FeedScopedId> includeAgencies = FilterValues.ofEmptyIsEverything(
    "includeAgencies",
    List.of()
  );
  private FilterValues<FeedScopedId> includeRoutes = FilterValues.ofEmptyIsEverything(
    "includeRoutes",
    List.of()
  );
  private FilterValues<FeedScopedId> includeServiceJourneys = FilterValues.ofEmptyIsEverything(
    "includeServiceJourneys",
    List.of()
  );
  private FilterValues<FeedScopedId> includeReplacementFor = FilterValues.ofEmptyIsEverything(
    "includeReplacementFor",
    List.of()
  );
  private FilterValues<String> includeNetexInternalPlanningCodes = FilterValues.ofEmptyIsEverything(
    "includeNetexInternalPlanningCodes",
    List.of()
  );
  private FilterValues<TripAlteration> includeAlterations = FilterValues.ofEmptyIsEverything(
    "includeAlterations",
    List.of()
  );
  private FilterValues<LocalDate> includeServiceDates = FilterValues.ofEmptyIsEverything(
    "includeServiceDates",
    List.of()
  );
  private List<FilterRequest<TripOnServiceDateSelectRequest>> filters = List.of();

  public TripOnServiceDateRequestBuilder withIncludeAgencies(FilterValues<FeedScopedId> agencies) {
    this.includeAgencies = agencies;
    return this;
  }

  public TripOnServiceDateRequestBuilder withIncludeRoutes(FilterValues<FeedScopedId> routes) {
    this.includeRoutes = routes;
    return this;
  }

  public TripOnServiceDateRequestBuilder withIncludeServiceJourneys(
    FilterValues<FeedScopedId> serviceJourneys
  ) {
    this.includeServiceJourneys = serviceJourneys;
    return this;
  }

  public TripOnServiceDateRequestBuilder withIncludeReplacementFor(
    FilterValues<FeedScopedId> replacementFor
  ) {
    this.includeReplacementFor = replacementFor;
    return this;
  }

  public TripOnServiceDateRequestBuilder withIncludeNetexInternalPlanningCodes(
    FilterValues<String> netexInternalPlanningCodes
  ) {
    this.includeNetexInternalPlanningCodes = netexInternalPlanningCodes;
    return this;
  }

  public TripOnServiceDateRequestBuilder withIncludeAlterations(
    FilterValues<TripAlteration> alterations
  ) {
    this.includeAlterations = alterations;
    return this;
  }

  public TripOnServiceDateRequestBuilder withIncludeServiceDates(
    FilterValues<LocalDate> serviceDates
  ) {
    this.includeServiceDates = serviceDates;
    return this;
  }

  public TripOnServiceDateRequestBuilder withFilters(
    List<FilterRequest<TripOnServiceDateSelectRequest>> filters
  ) {
    this.filters = filters;
    return this;
  }

  public TripOnServiceDateRequest build() {
    return new TripOnServiceDateRequest(
      includeServiceDates,
      includeAgencies,
      includeRoutes,
      includeServiceJourneys,
      includeReplacementFor,
      includeNetexInternalPlanningCodes,
      includeAlterations,
      filters
    );
  }
}
