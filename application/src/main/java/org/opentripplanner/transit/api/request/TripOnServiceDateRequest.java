package org.opentripplanner.transit.api.request;

import java.time.LocalDate;
import org.opentripplanner.transit.api.model.FilterValues;
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

  private final FilterValues<LocalDate> serviceDates;
  private final FilterValues<FeedScopedId> agencies;
  private final FilterValues<FeedScopedId> routes;
  private final FilterValues<FeedScopedId> serviceJourneys;
  private final FilterValues<FeedScopedId> replacementFor;
  private final FilterValues<String> netexInternalPlanningCodes;
  private final FilterValues<TripAlteration> alterations;

  TripOnServiceDateRequest(
    FilterValues<LocalDate> serviceDates,
    FilterValues<FeedScopedId> agencies,
    FilterValues<FeedScopedId> routes,
    FilterValues<FeedScopedId> serviceJourneys,
    FilterValues<FeedScopedId> replacementFor,
    FilterValues<String> netexInternalPlanningCodes,
    FilterValues<TripAlteration> alterations
  ) {
    this.serviceDates = serviceDates;
    this.agencies = agencies;
    this.routes = routes;
    this.serviceJourneys = serviceJourneys;
    this.replacementFor = replacementFor;
    this.netexInternalPlanningCodes = netexInternalPlanningCodes;
    this.alterations = alterations;
  }

  public static TripOnServiceDateRequestBuilder of(FilterValues<LocalDate> serviceDates) {
    return new TripOnServiceDateRequestBuilder(serviceDates);
  }

  public FilterValues<FeedScopedId> agencies() {
    return agencies;
  }

  public FilterValues<FeedScopedId> routes() {
    return routes;
  }

  public FilterValues<FeedScopedId> serviceJourneys() {
    return serviceJourneys;
  }

  public FilterValues<FeedScopedId> replacementFor() {
    return replacementFor;
  }

  public FilterValues<String> netexInternalPlanningCodes() {
    return netexInternalPlanningCodes;
  }

  public FilterValues<TripAlteration> alterations() {
    return alterations;
  }

  public FilterValues<LocalDate> serviceDates() {
    return serviceDates;
  }
}
