package org.opentripplanner.transit.api.request;

import java.time.LocalDate;
import java.util.List;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.timetable.TripAlteration;

public class TripOnServiceDateRequestBuilder {

  private List<FeedScopedId> authorities = List.of();
  private List<FeedScopedId> lines = List.of();
  private List<FeedScopedId> serviceJourneys = List.of();
  private List<FeedScopedId> replacementFor = List.of();
  private List<String> privateCodes = List.of();
  private List<TripAlteration> alterations = List.of();
  private final List<LocalDate> operatingDays;

  protected TripOnServiceDateRequestBuilder(List<LocalDate> operatingDays) {
    if (operatingDays == null || operatingDays.isEmpty()) {
      throw new IllegalArgumentException("operatingDays must have at least one date");
    }
    this.operatingDays = operatingDays;
  }

  public TripOnServiceDateRequestBuilder withAuthorities(List<FeedScopedId> authorities) {
    this.authorities = authorities;
    return this;
  }

  public TripOnServiceDateRequestBuilder withLines(List<FeedScopedId> lines) {
    this.lines = lines;
    return this;
  }

  public TripOnServiceDateRequestBuilder withServiceJourneys(List<FeedScopedId> serviceJourneys) {
    this.serviceJourneys = serviceJourneys;
    return this;
  }

  public TripOnServiceDateRequestBuilder withReplacementFor(List<FeedScopedId> replacementFor) {
    this.replacementFor = replacementFor;
    return this;
  }

  public TripOnServiceDateRequestBuilder withPrivateCodes(List<String> privateCodes) {
    this.privateCodes = privateCodes;
    return this;
  }

  public TripOnServiceDateRequestBuilder withAlterations(List<TripAlteration> alterations) {
    this.alterations = alterations;
    return this;
  }

  public TripOnServiceDateRequest build() {
    return new TripOnServiceDateRequest(
      authorities,
      lines,
      serviceJourneys,
      replacementFor,
      privateCodes,
      operatingDays,
      alterations
    );
  }
}
