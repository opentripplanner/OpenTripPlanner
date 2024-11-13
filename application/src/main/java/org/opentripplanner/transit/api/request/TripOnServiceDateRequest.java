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
    if (serviceDates.values() == null || serviceDates.values().isEmpty()) {
      throw new IllegalArgumentException("operatingDays must have at least one date");
    }
    this.serviceDates = CriteriaCollection.of(serviceDates);
    this.agencies = CriteriaCollection.of(agencies);
    this.routes = CriteriaCollection.of(routes);
    this.serviceJourneys = CriteriaCollection.of(serviceJourneys);
    this.replacementFor = CriteriaCollection.of(replacementFor);
    this.netexInternalPlanningCodes = CriteriaCollection.of(netexInternalPlanningCodes);
    this.alterations = CriteriaCollection.of(alterations);
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
