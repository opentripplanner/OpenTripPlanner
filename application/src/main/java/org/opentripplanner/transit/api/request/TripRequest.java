package org.opentripplanner.transit.api.request;

import java.time.LocalDate;
import org.opentripplanner.transit.api.model.FilterValues;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.timetable.Trip;

/**
 * A request for {@link Trip}s.
 * </p>
 * This request is used to retrieve {@link Trip}s that match the provided filter values.
 */
public class TripRequest {

  private final FilterValues<FeedScopedId> includeAgencies;
  private final FilterValues<FeedScopedId> includeRoutes;
  private final FilterValues<String> includeNetexInternalPlanningCodes;
  private final FilterValues<LocalDate> includeServiceDates;

  TripRequest(
    FilterValues<FeedScopedId> includeAgencies,
    FilterValues<FeedScopedId> includeRoutes,
    FilterValues<String> includeNetexInternalPlanningCodes,
    FilterValues<LocalDate> includeServiceDates
  ) {
    this.includeAgencies = includeAgencies;
    this.includeRoutes = includeRoutes;
    this.includeNetexInternalPlanningCodes = includeNetexInternalPlanningCodes;
    this.includeServiceDates = includeServiceDates;
  }

  public static TripRequestBuilder of() {
    return new TripRequestBuilder();
  }

  public FilterValues<FeedScopedId> includeAgencies() {
    return includeAgencies;
  }

  public FilterValues<FeedScopedId> includeRoutes() {
    return includeRoutes;
  }

  public FilterValues<String> includeNetexInternalPlanningCodes() {
    return includeNetexInternalPlanningCodes;
  }

  public FilterValues<LocalDate> includeServiceDates() {
    return includeServiceDates;
  }
}
