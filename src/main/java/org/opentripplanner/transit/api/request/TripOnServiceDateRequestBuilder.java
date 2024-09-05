package org.opentripplanner.transit.api.request;

import java.time.LocalDate;
import java.util.List;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.timetable.TripAlteration;

public class TripOnServiceDateRequestBuilder {

  private List<FeedScopedId> authorities;
  private List<FeedScopedId> lines;
  private List<FeedScopedId> serviceJourneys;
  private List<FeedScopedId> replacementFor;
  private List<String> privateCodes;
  private List<TripAlteration> alterations;
  private List<LocalDate> operatingDays;

  protected TripOnServiceDateRequestBuilder() {}

  public TripOnServiceDateRequestBuilder withOperatingDays(List<LocalDate> operatingDays) {
    this.operatingDays = operatingDays;
    return this;
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
      operatingDays,
      authorities,
      lines,
      serviceJourneys,
      replacementFor,
      privateCodes,
      alterations
    );
  }
}
