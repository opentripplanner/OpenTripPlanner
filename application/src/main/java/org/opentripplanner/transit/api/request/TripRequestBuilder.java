package org.opentripplanner.transit.api.request;

import java.time.LocalDate;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.transit.api.model.FilterValues;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class TripRequestBuilder {

  private FilterValues<FeedScopedId> includeAgencies = FilterValues.ofNullIsEverything(
    "includeAgencies",
    null
  );
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
    return this;
  }

  public TripRequestBuilder withIncludeRoutes(@Nullable List<FeedScopedId> includeRoutes) {
    this.includeRoutes = FilterValues.ofNullIsEverything("includeRoutes", includeRoutes);
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
      includeAgencies,
      includeRoutes,
      includeNetexInternalPlanningCodes,
      includeServiceDates
    );
  }
}
