package org.opentripplanner.transit.api.request;

import java.time.LocalDate;
import java.util.List;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.timetable.TripAlteration;

/*
 * A request for trips on a specific service date.
 *
 * This request is used to retrieve TripsOnServiceDates that match the provided criteria.
 * At least one operatingDay must be provided.
 */
public class TripOnServiceDateRequest {

  private final List<FeedScopedId> authorities;
  private final List<FeedScopedId> lines;
  private final List<FeedScopedId> serviceJourneys;
  private final List<FeedScopedId> replacementFor;
  private final List<String> privateCodes;
  private final List<TripAlteration> alterations;
  private final List<LocalDate> operatingDays;

  protected TripOnServiceDateRequest(
    List<FeedScopedId> authorities,
    List<FeedScopedId> lines,
    List<FeedScopedId> serviceJourneys,
    List<FeedScopedId> replacementFor,
    List<String> privateCodes,
    List<LocalDate> operatingDays,
    List<TripAlteration> alterations
  ) {
    this.authorities = List.copyOf(authorities);
    this.lines = List.copyOf(lines);
    this.serviceJourneys = List.copyOf(serviceJourneys);
    this.replacementFor = List.copyOf(replacementFor);
    this.privateCodes = List.copyOf(privateCodes);
    this.alterations = List.copyOf(alterations);
    this.operatingDays = List.copyOf(operatingDays);
  }

  public static TripOnServiceDateRequestBuilder of(List<LocalDate> operatingDays) {
    return new TripOnServiceDateRequestBuilder(operatingDays);
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
