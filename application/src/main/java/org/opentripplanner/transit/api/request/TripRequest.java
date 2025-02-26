package org.opentripplanner.transit.api.request;

import java.time.LocalDate;
import java.util.Objects;
import org.opentripplanner.transit.api.model.FilterValues;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.timetable.Trip;

/**
 * A request for {@link Trip}s.
 * </p>
 * This request is used to retrieve {@link Trip}s that match the provided filter values.
 */
public class TripRequest {

  private final FilterValues<FeedScopedId> includedAgencies;
  private final FilterValues<FeedScopedId> includedRoutes;

  private final FilterValues<FeedScopedId> excludedAgencies;
  private final FilterValues<FeedScopedId> excludedRoutes;

  private final FilterValues<String> netexInternalPlanningCodes;
  private final FilterValues<LocalDate> serviceDates;

  TripRequest(
    FilterValues<FeedScopedId> includedAgencies,
    FilterValues<FeedScopedId> includedRoutes,
    FilterValues<FeedScopedId> excludedAgencies,
    FilterValues<FeedScopedId> excludedRoutes,
    FilterValues<String> netexInternalPlanningCodes,
    FilterValues<LocalDate> serviceDates
  ) {
    this.includedAgencies = includedAgencies;
    this.includedRoutes = includedRoutes;
    this.excludedAgencies = excludedAgencies;
    this.excludedRoutes = excludedRoutes;
    this.netexInternalPlanningCodes = netexInternalPlanningCodes;
    this.serviceDates = serviceDates;
  }

  public static TripRequestBuilder of() {
    return new TripRequestBuilder();
  }

  public FilterValues<FeedScopedId> includedAgencies() {
    return includedAgencies;
  }

  public FilterValues<FeedScopedId> includedRoutes() {
    return includedRoutes;
  }

  public FilterValues<FeedScopedId> excludedAgencies() {
    return excludedAgencies;
  }

  public FilterValues<FeedScopedId> excludedRoutes() {
    return excludedRoutes;
  }

  public FilterValues<String> netexInternalPlanningCodes() {
    return netexInternalPlanningCodes;
  }

  public FilterValues<LocalDate> serviceDates() {
    return serviceDates;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof TripRequest that)) return false;
    return (
      Objects.equals(includedAgencies, that.includedAgencies) &&
      Objects.equals(includedRoutes, that.includedRoutes) &&
      Objects.equals(excludedAgencies, that.excludedAgencies) &&
      Objects.equals(excludedRoutes, that.excludedRoutes) &&
      Objects.equals(netexInternalPlanningCodes, that.netexInternalPlanningCodes) &&
      Objects.equals(serviceDates, that.serviceDates)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(
      includedAgencies,
      includedRoutes,
      excludedAgencies,
      excludedRoutes,
      netexInternalPlanningCodes,
      serviceDates
    );
  }
}
