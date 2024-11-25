package org.opentripplanner.transit.api.request;

import java.time.LocalDate;
import org.opentripplanner.transit.api.model.FilterValueCollection;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.timetable.Trip;

/**
 * A request for {@link Trip}s.
 * </p>
 * This request is used to retrieve {@link Trip}s that match the provided filter values.
 */
public class TripRequest {

  private final FilterValueCollection<FeedScopedId> agencies;
  private final FilterValueCollection<FeedScopedId> routes;
  private final FilterValueCollection<String> netexInternalPlanningCodes;
  private final FilterValueCollection<LocalDate> serviceDates;

  protected TripRequest(
    FilterValueCollection<FeedScopedId> agencies,
    FilterValueCollection<FeedScopedId> routes,
    FilterValueCollection<String> netexInternalPlanningCodes,
    FilterValueCollection<LocalDate> serviceDates
  ) {
    this.agencies = agencies;
    this.routes = routes;
    this.netexInternalPlanningCodes = netexInternalPlanningCodes;
    this.serviceDates = serviceDates;
  }

  public static TripRequestBuilder of() {
    return new TripRequestBuilder();
  }

  public FilterValueCollection<FeedScopedId> agencies() {
    return agencies;
  }

  public FilterValueCollection<FeedScopedId> routes() {
    return routes;
  }

  public FilterValueCollection<String> netexInternalPlanningCodes() {
    return netexInternalPlanningCodes;
  }

  public FilterValueCollection<LocalDate> serviceDates() {
    return serviceDates;
  }
}
