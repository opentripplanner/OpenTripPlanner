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

  private final FilterValues<FeedScopedId> agencies;
  private final FilterValues<FeedScopedId> routes;
  private final FilterValues<String> netexInternalPlanningCodes;
  private final FilterValues<LocalDate> serviceDates;

  TripRequest(
    FilterValues<FeedScopedId> agencies,
    FilterValues<FeedScopedId> routes,
    FilterValues<String> netexInternalPlanningCodes,
    FilterValues<LocalDate> serviceDates
  ) {
    this.agencies = agencies;
    this.routes = routes;
    this.netexInternalPlanningCodes = netexInternalPlanningCodes;
    this.serviceDates = serviceDates;
  }

  public static TripRequestBuilder of() {
    return new TripRequestBuilder();
  }

  public FilterValues<FeedScopedId> agencies() {
    return agencies;
  }

  public FilterValues<FeedScopedId> routes() {
    return routes;
  }

  public FilterValues<String> netexInternalPlanningCodes() {
    return netexInternalPlanningCodes;
  }

  public FilterValues<LocalDate> serviceDates() {
    return serviceDates;
  }
}
