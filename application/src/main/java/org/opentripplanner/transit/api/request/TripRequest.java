package org.opentripplanner.transit.api.request;

import java.time.LocalDate;
import org.opentripplanner.transit.api.model.CriteriaCollection;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.timetable.Trip;

/**
 * A request for {@link Trip}s.
 * </p>
 * This request is used to retrieve {@link Trip}s that match the provided criteria.
 */
public class TripRequest {

  private final CriteriaCollection<FeedScopedId> agencies;
  private final CriteriaCollection<FeedScopedId> routes;
  private final CriteriaCollection<String> netexInternalPlanningCodes;
  private final CriteriaCollection<LocalDate> serviceDates;

  protected TripRequest(
    CriteriaCollection<FeedScopedId> agencies,
    CriteriaCollection<FeedScopedId> routes,
    CriteriaCollection<String> netexInternalPlanningCodes,
    CriteriaCollection<LocalDate> serviceDates
  ) {
    this.agencies = CriteriaCollection.ofEmptyIsEverything(agencies);
    this.routes = CriteriaCollection.ofEmptyIsEverything(routes);
    this.netexInternalPlanningCodes =
      CriteriaCollection.ofEmptyIsEverything(netexInternalPlanningCodes);
    this.serviceDates = CriteriaCollection.ofEmptyIsEverything(serviceDates);
  }

  public static TripRequestBuilder of() {
    return new TripRequestBuilder();
  }

  public CriteriaCollection<FeedScopedId> agencies() {
    return agencies;
  }

  public CriteriaCollection<FeedScopedId> routes() {
    return routes;
  }

  public CriteriaCollection<String> netexInternalPlanningCodes() {
    return netexInternalPlanningCodes;
  }

  public CriteriaCollection<LocalDate> serviceDates() {
    return serviceDates;
  }
}
