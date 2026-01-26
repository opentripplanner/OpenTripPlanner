package org.opentripplanner.transit.api.request;

import java.time.LocalDate;
import java.util.List;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.api.model.FilterValues;
import org.opentripplanner.transit.model.basic.TransitMode;
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
  private FilterValues<TransitMode> includeModes = FilterValues.ofEmptyIsEverything(
    "modes",
    List.of()
  );
  private FilterValues<TransitMode> excludeModes = FilterValues.ofEmptyIsEverything(
    "excludeModes",
    List.of()
  );

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

  public TripOnServiceDateRequestBuilder withIncludeModes(FilterValues<TransitMode> modes) {
    this.includeModes = modes;
    return this;
  }

  public TripOnServiceDateRequestBuilder withExcludeModes(FilterValues<TransitMode> modes) {
    this.excludeModes = modes;
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
      includeModes,
      excludeModes
    );
  }
}
