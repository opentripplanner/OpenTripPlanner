package org.opentripplanner.transit.api.request;

import java.time.LocalDate;
import org.opentripplanner.transit.api.model.FilterValueCollection;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.timetable.TripAlteration;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;

/**
 * A request for trips on a specific service date.
 * </p>
 * This request is used to retrieve {@link TripOnServiceDate}s that match the provided filter values.
 * At least one operatingDay must be provided.
 */
public class TripOnServiceDateRequest {

  private final FilterValueCollection<LocalDate> serviceDates;
  private final FilterValueCollection<FeedScopedId> agencies;
  private final FilterValueCollection<FeedScopedId> routes;
  private final FilterValueCollection<FeedScopedId> serviceJourneys;
  private final FilterValueCollection<FeedScopedId> replacementFor;
  private final FilterValueCollection<String> netexInternalPlanningCodes;
  private final FilterValueCollection<TripAlteration> alterations;

  protected TripOnServiceDateRequest(
    FilterValueCollection<LocalDate> serviceDates,
    FilterValueCollection<FeedScopedId> agencies,
    FilterValueCollection<FeedScopedId> routes,
    FilterValueCollection<FeedScopedId> serviceJourneys,
    FilterValueCollection<FeedScopedId> replacementFor,
    FilterValueCollection<String> netexInternalPlanningCodes,
    FilterValueCollection<TripAlteration> alterations
  ) {
    if (serviceDates == null || serviceDates.get() == null || serviceDates.get().isEmpty()) {
      throw new IllegalArgumentException("operatingDays must have at least one date");
    }
    this.serviceDates = serviceDates;
    this.agencies = agencies;
    this.routes = routes;
    this.serviceJourneys = serviceJourneys;
    this.replacementFor = replacementFor;
    this.netexInternalPlanningCodes = netexInternalPlanningCodes;
    this.alterations = alterations;
  }

  public static TripOnServiceDateRequestBuilder of() {
    return new TripOnServiceDateRequestBuilder();
  }

  public FilterValueCollection<FeedScopedId> agencies() {
    return agencies;
  }

  public FilterValueCollection<FeedScopedId> routes() {
    return routes;
  }

  public FilterValueCollection<FeedScopedId> serviceJourneys() {
    return serviceJourneys;
  }

  public FilterValueCollection<FeedScopedId> replacementFor() {
    return replacementFor;
  }

  public FilterValueCollection<String> netexInternalPlanningCodes() {
    return netexInternalPlanningCodes;
  }

  public FilterValueCollection<TripAlteration> alterations() {
    return alterations;
  }

  public FilterValueCollection<LocalDate> serviceDates() {
    return serviceDates;
  }
}
