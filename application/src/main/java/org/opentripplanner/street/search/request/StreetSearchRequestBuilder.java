package org.opentripplanner.street.search.request;

import java.time.Instant;
import java.util.function.Consumer;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.routing.api.request.preference.RoutingPreferencesBuilder;

public class StreetSearchRequestBuilder {

  Instant startTime;
  StreetMode mode;
  RoutingPreferences preferences;
  boolean arriveBy;
  boolean wheelchair;
  GenericLocation from;
  GenericLocation to;

  StreetSearchRequestBuilder(StreetSearchRequest original) {
    this.startTime = original.startTime();
    this.mode = original.mode();
    this.preferences = original.preferences();
    this.arriveBy = original.arriveBy();
    this.wheelchair = original.wheelchair();
    this.from = original.from();
    this.to = original.to();
  }

  public StreetSearchRequestBuilder withStartTime(Instant startTime) {
    this.startTime = startTime;
    return this;
  }

  public StreetSearchRequestBuilder withMode(StreetMode mode) {
    this.mode = mode;
    return this;
  }

  public StreetSearchRequestBuilder withPreferences(RoutingPreferences preferences) {
    this.preferences = preferences;
    return this;
  }

  public StreetSearchRequestBuilder withPreferences(Consumer<RoutingPreferencesBuilder> body) {
    return withPreferences(preferences.copyOf().apply(body).build());
  }

  public StreetSearchRequestBuilder withArriveBy(boolean arriveBy) {
    this.arriveBy = arriveBy;
    return this;
  }

  public StreetSearchRequestBuilder withWheelchair(boolean wheelchair) {
    this.wheelchair = wheelchair;
    return this;
  }

  public StreetSearchRequestBuilder withFrom(GenericLocation from) {
    this.from = from;
    return this;
  }

  public StreetSearchRequestBuilder withTo(GenericLocation to) {
    this.to = to;
    return this;
  }

  Instant startTimeOrNow() {
    return startTime == null ? Instant.now() : startTime;
  }

  public StreetSearchRequest build() {
    return new StreetSearchRequest(this);
  }
}
