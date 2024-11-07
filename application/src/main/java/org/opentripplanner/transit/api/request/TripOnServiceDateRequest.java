package org.opentripplanner.transit.api.request;

import java.time.LocalDate;
import java.util.List;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.timetable.TripAlteration;
import org.opentripplanner.utils.collection.ListUtils;

/*
 * A request for trips on a specific service date.
 *
 * This request is used to retrieve TripsOnServiceDates that match the provided criteria.
 * At least one operatingDay must be provided.
 */
public class TripOnServiceDateRequest {

  private final List<LocalDate> operatingDays;
  private final List<FeedScopedId> authorities;
  private final List<FeedScopedId> lines;
  private final List<FeedScopedId> serviceJourneys;
  private final List<FeedScopedId> replacementFor;
  private final List<String> privateCodes;
  private final List<TripAlteration> alterations;

  protected TripOnServiceDateRequest(
    List<LocalDate> operatingDays,
    List<FeedScopedId> authorities,
    List<FeedScopedId> lines,
    List<FeedScopedId> serviceJourneys,
    List<FeedScopedId> replacementFor,
    List<String> privateCodes,
    List<TripAlteration> alterations
  ) {
    if (operatingDays == null || operatingDays.isEmpty()) {
      throw new IllegalArgumentException("operatingDays must have at least one date");
    }
    this.operatingDays = ListUtils.nullSafeImmutableList(operatingDays);
    this.authorities = ListUtils.nullSafeImmutableList(authorities);
    this.lines = ListUtils.nullSafeImmutableList(lines);
    this.serviceJourneys = ListUtils.nullSafeImmutableList(serviceJourneys);
    this.replacementFor = ListUtils.nullSafeImmutableList(replacementFor);
    this.privateCodes = ListUtils.nullSafeImmutableList(privateCodes);
    this.alterations = ListUtils.nullSafeImmutableList(alterations);
  }

  public static TripOnServiceDateRequestBuilder of() {
    return new TripOnServiceDateRequestBuilder();
  }

  public List<FeedScopedId> authorities() {
    return authorities;
  }

  public List<FeedScopedId> lines() {
    return lines;
  }

  public List<FeedScopedId> serviceJourneys() {
    return serviceJourneys;
  }

  public List<FeedScopedId> replacementFor() {
    return replacementFor;
  }

  public List<String> privateCodes() {
    return privateCodes;
  }

  public List<TripAlteration> alterations() {
    return alterations;
  }

  public List<LocalDate> operatingDays() {
    return operatingDays;
  }
}
