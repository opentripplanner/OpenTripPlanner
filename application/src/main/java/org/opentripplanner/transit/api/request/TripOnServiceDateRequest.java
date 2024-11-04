package org.opentripplanner.transit.api.request;

import java.time.LocalDate;
import java.util.List;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.timetable.TripAlteration;
import org.opentripplanner.utils.collection.ListUtils;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;

/**
 * A request for trips on a specific service date.
 *
 * This request is used to retrieve {@link TripOnServiceDate}s that match the provided criteria.
 * At least one operatingDay must be provided.
 */
public class TripOnServiceDateRequest {

  private final List<LocalDate> serviceDates;
  private final List<FeedScopedId> agencies;
  private final List<FeedScopedId> routes;
  private final List<FeedScopedId> serviceJourneys;
  private final List<FeedScopedId> replacementFor;
  private final List<String> netexInternalPlanningCodes;
  private final List<TripAlteration> alterations;

  protected TripOnServiceDateRequest(
    List<LocalDate> serviceDates,
    List<FeedScopedId> agencies,
    List<FeedScopedId> routes,
    List<FeedScopedId> serviceJourneys,
    List<FeedScopedId> replacementFor,
    List<String> netexInternalPlanningCodes,
    List<TripAlteration> alterations
  ) {
    if (serviceDates == null || serviceDates.isEmpty()) {
      throw new IllegalArgumentException("operatingDays must have at least one date");
    }
    this.serviceDates = ListUtils.nullSafeImmutableList(serviceDates);
    this.agencies = ListUtils.nullSafeImmutableList(agencies);
    this.routes = ListUtils.nullSafeImmutableList(routes);
    this.serviceJourneys = ListUtils.nullSafeImmutableList(serviceJourneys);
    this.replacementFor = ListUtils.nullSafeImmutableList(replacementFor);
    this.netexInternalPlanningCodes = ListUtils.nullSafeImmutableList(netexInternalPlanningCodes);
    this.alterations = ListUtils.nullSafeImmutableList(alterations);
  }

  public static TripOnServiceDateRequestBuilder of() {
    return new TripOnServiceDateRequestBuilder();
  }

  public List<FeedScopedId> agencies() {
    return agencies;
  }

  public List<FeedScopedId> routes() {
    return routes;
  }

  public List<FeedScopedId> serviceJourneys() {
    return serviceJourneys;
  }

  public List<FeedScopedId> replacementFor() {
    return replacementFor;
  }

  public List<String> netexInternalPlanningCodes() {
    return netexInternalPlanningCodes;
  }

  public List<TripAlteration> alterations() {
    return alterations;
  }

  public List<LocalDate> serviceDates() {
    return serviceDates;
  }
}
