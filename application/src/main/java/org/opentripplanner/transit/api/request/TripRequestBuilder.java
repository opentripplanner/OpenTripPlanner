package org.opentripplanner.transit.api.request;

import java.time.LocalDate;
import java.util.List;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class TripRequestBuilder {

  private List<FeedScopedId> authorities;
  private List<FeedScopedId> lines;
  private List<String> privateCodes;
  private List<LocalDate> activeDates;

  protected TripRequestBuilder() {}

  public TripRequestBuilder withAuthorities(List<FeedScopedId> authorities) {
    this.authorities = authorities;
    return this;
  }

  public TripRequestBuilder withLines(List<FeedScopedId> lines) {
    this.lines = lines;
    return this;
  }

  public TripRequestBuilder withPrivateCodes(List<String> privateCodes) {
    this.privateCodes = privateCodes;
    return this;
  }

  public TripRequestBuilder withActiveDates(List<LocalDate> activeDates) {
    this.activeDates = activeDates;
    return this;
  }

  public TripRequest build() {
    return new TripRequest(authorities, lines, privateCodes, activeDates);
  }
}
