package org.opentripplanner.transit.api.request;

import java.time.LocalDate;
import java.util.List;
import org.opentripplanner.framework.collection.ListUtils;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.timetable.Trip;

/**
 * A request for {@link Trip}s.
 *
 * This request is used to retrieve {@link Trip}s that match the provided criteria.
 */
public class TripRequest {

  private final List<FeedScopedId> agencies;
  private final List<FeedScopedId> routes;
  private final List<String> netexInternalPlanningCode;
  private final List<LocalDate> serviceDates;

  protected TripRequest(
    List<FeedScopedId> agencies,
    List<FeedScopedId> routes,
    List<String> netexInternalPlanningCodes,
    List<LocalDate> serviceDates
  ) {
    this.agencies = ListUtils.nullSafeImmutableList(agencies);
    this.routes = ListUtils.nullSafeImmutableList(routes);
    this.netexInternalPlanningCode = ListUtils.nullSafeImmutableList(netexInternalPlanningCodes);
    this.serviceDates = ListUtils.nullSafeImmutableList(serviceDates);
  }

  public static TripRequestBuilder of() {
    return new TripRequestBuilder();
  }

  public List<FeedScopedId> agencies() {
    return agencies;
  }

  public List<FeedScopedId> routes() {
    return routes;
  }

  public List<String> netexInternalPlanningCodes() {
    return netexInternalPlanningCode;
  }

  public List<LocalDate> serviceDates() {
    return serviceDates;
  }
}
