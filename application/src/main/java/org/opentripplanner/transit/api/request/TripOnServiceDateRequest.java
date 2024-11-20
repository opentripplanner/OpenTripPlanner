package org.opentripplanner.transit.api.request;

import java.time.LocalDate;
import org.opentripplanner.transit.api.model.CriteriaCollection;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.timetable.TripAlteration;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;

/**
 * A request for trips on a specific service date.
 * </p>
 * This request is used to retrieve {@link TripOnServiceDate}s that match the provided criteria.
 * At least one operatingDay must be provided.
 */
public class TripOnServiceDateRequest {

  private final CriteriaCollection<LocalDate> serviceDates;
  private final CriteriaCollection<FeedScopedId> agencies;
  private final CriteriaCollection<FeedScopedId> routes;
  private final CriteriaCollection<FeedScopedId> serviceJourneys;
  private final CriteriaCollection<FeedScopedId> replacementFor;
  private final CriteriaCollection<String> netexInternalPlanningCodes;
  private final CriteriaCollection<TripAlteration> alterations;

  protected TripOnServiceDateRequest(
    CriteriaCollection<LocalDate> serviceDates,
    CriteriaCollection<FeedScopedId> agencies,
    CriteriaCollection<FeedScopedId> routes,
    CriteriaCollection<FeedScopedId> serviceJourneys,
    CriteriaCollection<FeedScopedId> replacementFor,
    CriteriaCollection<String> netexInternalPlanningCodes,
    CriteriaCollection<TripAlteration> alterations
  ) {
    if (serviceDates.get() == null || serviceDates.get().isEmpty()) {
      throw new IllegalArgumentException("operatingDays must have at least one date");
    }
    this.serviceDates = CriteriaCollection.ofEmptyIsEverything(serviceDates);
    this.agencies = CriteriaCollection.ofEmptyIsEverything(agencies);
    this.routes = CriteriaCollection.ofEmptyIsEverything(routes);
    this.serviceJourneys = CriteriaCollection.ofEmptyIsEverything(serviceJourneys);
    this.replacementFor = CriteriaCollection.ofEmptyIsEverything(replacementFor);
    this.netexInternalPlanningCodes =
      CriteriaCollection.ofEmptyIsEverything(netexInternalPlanningCodes);
    this.alterations = CriteriaCollection.ofEmptyIsEverything(alterations);
  }

  public static TripOnServiceDateRequestBuilder of() {
    return new TripOnServiceDateRequestBuilder();
  }

  public CriteriaCollection<FeedScopedId> agencies() {
    return agencies;
  }

  public CriteriaCollection<FeedScopedId> routes() {
    return routes;
  }

  public CriteriaCollection<FeedScopedId> serviceJourneys() {
    return serviceJourneys;
  }

  public CriteriaCollection<FeedScopedId> replacementFor() {
    return replacementFor;
  }

  public CriteriaCollection<String> netexInternalPlanningCodes() {
    return netexInternalPlanningCodes;
  }

  public CriteriaCollection<TripAlteration> alterations() {
    return alterations;
  }

  public CriteriaCollection<LocalDate> serviceDates() {
    return serviceDates;
  }
}
