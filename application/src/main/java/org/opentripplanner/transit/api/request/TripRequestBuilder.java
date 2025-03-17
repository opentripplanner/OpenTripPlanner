package org.opentripplanner.transit.api.request;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.transit.api.model.FilterValues;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class TripRequestBuilder {

  private FilterValues<FeedScopedId> includedAgencies = FilterValues.ofEmptyIsEverything(
    "includedAgencies",
    List.of()
  );
  private FilterValues<FeedScopedId> includedRoutes = FilterValues.ofEmptyIsEverything(
    "includedRoutes",
    List.of()
  private FilterValues<FeedScopedId> includeAgencies = FilterValues.ofNullIsEverything(
    "includeAgencies",
    null
  );

  private FilterValues<FeedScopedId> excludedAgencies = FilterValues.ofEmptyIsEverything(
    "excludedAgencies",
    List.of()
  );
  private FilterValues<FeedScopedId> excludedRoutes = FilterValues.ofEmptyIsEverything(
    "excludedRoutes",
    List.of()
  );

  private FilterValues<String> netexInternalPlanningCodes = FilterValues.ofEmptyIsEverything(
    "netexInternalPlanningCodes",
    List.of()
  private FilterValues<FeedScopedId> includeRoutes = FilterValues.ofNullIsEverything(
    "includeRoutes",
    null
  );
  private FilterValues<String> includeNetexInternalPlanningCodes = FilterValues.ofNullIsEverything(
    "includeNetexInternalPlanningCodes",
    null
  );
  private FilterValues<LocalDate> includeServiceDates = FilterValues.ofNullIsEverything(
    "includeServiceDates",
    null
  );

  TripRequestBuilder() {}

  public TripRequestBuilder withIncludeAgencies(@Nullable List<FeedScopedId> includeAgencies) {
    this.includeAgencies = FilterValues.ofNullIsEverything("includeAgencies", includeAgencies);
  public TripRequestBuilder withIncludedAgencies(Collection<FeedScopedId> agencies) {
    this.includedAgencies = FilterValues.ofEmptyIsNothing("includedAgencies", agencies);
    return this;
  }

  public TripRequestBuilder withIncludeRoutes(@Nullable List<FeedScopedId> includeRoutes) {
    this.includeRoutes = FilterValues.ofNullIsEverything("includeRoutes", includeRoutes);
  public TripRequestBuilder withIncludedRoutes(Collection<FeedScopedId> routes) {
    this.includedRoutes = FilterValues.ofEmptyIsNothing("includedRoutes", routes);
    return this;
  }

  public TripRequestBuilder withExcludedAgencies(Collection<FeedScopedId> agencies) {
    this.excludedAgencies = FilterValues.ofEmptyIsEverything("excludedAgencies", agencies);
    return this;
  }

  public TripRequestBuilder withExcludedRoutes(Collection<FeedScopedId> routes) {
    this.excludedRoutes = FilterValues.ofEmptyIsEverything("excludedRoutes", routes);
    return this;
  }

  public TripRequestBuilder withIncludeNetexInternalPlanningCodes(
    @Nullable List<String> includeNetexInternalPlanningCodes
  ) {
    this.includeNetexInternalPlanningCodes = FilterValues.ofNullIsEverything(
      "includeNetexInternalPlanningCodes",
      includeNetexInternalPlanningCodes
    );
    return this;
  }

  public TripRequestBuilder withIncludeServiceDates(@Nullable List<LocalDate> includeServiceDates) {
    this.includeServiceDates = FilterValues.ofNullIsEverything(
      "includeServiceDates",
      includeServiceDates
    );
    return this;
  }

  public TripRequest build() {
    return new TripRequest(
      includedAgencies,
      includedRoutes,
      excludedAgencies,
      excludedRoutes,
      netexInternalPlanningCodes,
      includeServiceDates
    );
  }
}
