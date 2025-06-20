package org.opentripplanner.routing.api.request;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.routing.api.request.preference.RoutingPreferencesBuilder;
import org.opentripplanner.routing.api.request.request.JourneyRequest;

/**
 * Trip planning request with a list of via points.
 *
 * @deprecated We will replace the complex via-search with a simpler version part of the
 *      existing trip search.
 */
@Deprecated
public class RouteViaRequest implements Serializable {

  private final GenericLocation from;
  private final List<ViaSegment> viaSegments;
  private final GenericLocation to;
  private final Instant dateTime;
  private final Duration searchWindow;
  private final boolean wheelchair;
  private final RoutingPreferences preferences;
  private final Locale locale;
  private final Integer numItineraries;

  private RouteViaRequest(
    List<ViaLocationDeprecated> viaLocations,
    List<JourneyRequest> viaJourneys
  ) {
    if (viaLocations == null || viaLocations.isEmpty()) {
      throw new IllegalArgumentException("viaLocations must not be empty");
    }

    if (
      viaJourneys == null || viaJourneys.isEmpty() || viaJourneys.size() != viaLocations.size() + 1
    ) {
      throw new IllegalArgumentException("There must be one more JourneyRequest than ViaLocation");
    }

    this.from = null;
    this.viaSegments = new ArrayList<>();
    this.to = null;
    this.dateTime = Instant.now();
    this.searchWindow = null;
    this.wheelchair = false;
    this.preferences = RoutingPreferences.DEFAULT;
    this.locale = null;
    this.numItineraries = null;

    // Last ViaSegment has no ViaLocation
    for (int i = 0; i < viaJourneys.size(); i++) {
      var viaLocation = i < viaJourneys.size() - 1 ? viaLocations.get(i) : null;
      viaSegments.add(new ViaSegment(viaJourneys.get(i), viaLocation));
    }
  }

  private RouteViaRequest(Builder builder) {
    this.from = Objects.requireNonNull(builder.from);
    this.viaSegments = Objects.requireNonNull(builder.viaSegments);
    this.to = Objects.requireNonNull(builder.to);
    this.dateTime = Objects.requireNonNull(builder.dateTime);
    this.searchWindow = Objects.requireNonNull(builder.searchWindow);
    this.wheelchair = builder.wheelchair;
    this.locale = builder.locale;
    this.preferences = Objects.requireNonNull(builder.preferences);
    this.numItineraries = builder.numItineraries;
  }

  public static Builder of(
    List<ViaLocationDeprecated> viaLocations,
    List<JourneyRequest> viaJourneys
  ) {
    return new Builder(new RouteViaRequest(viaLocations, viaJourneys));
  }

  public Builder copyOf() {
    return new Builder(this);
  }

  public RouteRequestBuilder routeRequestFirstSegment() {
    // TODO: This should use the configured default, not the code default. This will lead to errors
    //       where a deployment would expect the same perferences in the via is in a normal routing
    //       request.
    var request = RouteRequest.of();

    request.withFrom(from);
    request.withSearchWindow(searchWindow);
    request.withDateTime(dateTime);
    request.withPreferences(preferences);
    if (numItineraries != null) {
      request.withNumItineraries(numItineraries);
    }
    request.withJourney(j -> j.withWheelchair(wheelchair));
    return request;
  }

  public GenericLocation from() {
    return from;
  }

  public List<ViaSegment> viaSegment() {
    return viaSegments;
  }

  public GenericLocation to() {
    return to;
  }

  public Instant dateTime() {
    return dateTime;
  }

  public Duration searchWindow() {
    return searchWindow;
  }

  public boolean wheelchair() {
    return wheelchair;
  }

  public RoutingPreferences preferences() {
    return preferences;
  }

  public Locale locale() {
    return locale;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof RouteViaRequest other)) {
      return false;
    }

    return (
      viaSegments.equals(other.viaSegments) &&
      from.equals(other.from) &&
      to.equals(other.to) &&
      dateTime.equals(other.dateTime) &&
      searchWindow.equals(other.searchWindow) &&
      wheelchair == other.wheelchair &&
      Objects.equals(locale, other.locale) &&
      preferences.equals(other.preferences) &&
      Objects.equals(numItineraries, other.numItineraries)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(
      viaSegments,
      from,
      to,
      dateTime,
      searchWindow,
      wheelchair,
      locale,
      preferences,
      numItineraries
    );
  }

  public static class Builder {

    private final List<ViaSegment> viaSegments;
    private GenericLocation from;
    private GenericLocation to;
    private Instant dateTime;
    private Duration searchWindow;
    private Locale locale;
    private boolean wheelchair;
    private RoutingPreferences preferences;
    private Integer numItineraries;

    public Builder(RouteViaRequest original) {
      this.from = original.from;
      this.to = original.to;
      this.dateTime = original.dateTime;
      this.searchWindow = original.searchWindow;
      this.wheelchair = original.wheelchair;
      this.preferences = original.preferences;
      this.viaSegments = original.viaSegments;
      this.numItineraries = original.numItineraries;
    }

    public RouteViaRequest build() {
      return new RouteViaRequest(this);
    }

    public Builder withFrom(GenericLocation from) {
      this.from = from;
      return this;
    }

    public Builder withTo(GenericLocation to) {
      this.to = to;
      return this;
    }

    public Builder withDateTime(Instant dateTime) {
      this.dateTime = dateTime;
      return this;
    }

    public Builder withSearchWindow(Duration searchWindow) {
      this.searchWindow = searchWindow;
      return this;
    }

    public Builder withLocale(Locale locale) {
      this.locale = locale;
      return this;
    }

    public Builder withWheelchair(boolean wheelchair) {
      this.wheelchair = wheelchair;
      return this;
    }

    public Builder withPreferences(Consumer<RoutingPreferencesBuilder> prefs) {
      preferences = preferences.copyOf().apply(prefs).build();
      return this;
    }

    public Builder withNumItineraries(Integer numItineraries) {
      this.numItineraries = numItineraries;
      return this;
    }
  }

  /**
   * ViaSegments contains the {@link JourneyRequest} to the next {@link ViaLocationDeprecated}. The last
   * segment has null viaLocation, as `to` is the destination of that segment.
   */
  public record ViaSegment(JourneyRequest journeyRequest, ViaLocationDeprecated viaLocation) {}
}
