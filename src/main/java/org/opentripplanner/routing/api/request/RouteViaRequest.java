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
import org.opentripplanner.routing.api.request.request.JourneyRequest;

/**
 * Trip planning request with a list of via points.
 */
public class RouteViaRequest implements Serializable {

  private List<ViaLeg> viaLegs = new ArrayList<>();
  private GenericLocation from = new GenericLocation(null, null);
  private GenericLocation to = new GenericLocation(null, null);
  private Instant dateTime = Instant.now();
  private Duration searchWindow;
  private boolean wheelchair = false;
  private RoutingPreferences preferences = new RoutingPreferences();

  private RouteViaRequest(List<ViaLocation> viaLocations, List<JourneyRequest> viaJourneys) {
    if (viaLocations == null || viaLocations.isEmpty()) {
      throw new IllegalArgumentException("viaLocations must not be empty");
    }

    if (
      viaJourneys == null || viaJourneys.isEmpty() || viaJourneys.size() != viaLocations.size() + 1
    ) {
      throw new IllegalArgumentException("There must be one more JourneyRequest than ViaLocation");
    }

    // Last ViaLeg has no ViaLocation
    for (int i = 0; i < viaJourneys.size(); i++) {
      var viaLocation = i < viaJourneys.size() - 1 ? viaLocations.get(i) : null;
      viaLegs.add(new ViaLeg(viaJourneys.get(i), viaLocation));
    }
  }

  private RouteViaRequest(Builder builder) {
    this.viaLegs = Objects.requireNonNull(builder.viaLegs);
    this.from = Objects.requireNonNull(builder.from);
    this.to = Objects.requireNonNull(builder.to);
    this.dateTime = Objects.requireNonNull(builder.dateTime);
    this.searchWindow = Objects.requireNonNull(builder.searchWindow);
    this.wheelchair = builder.wheelchair;
    this.preferences = Objects.requireNonNull(builder.preferences);
  }

  public static Builder of(List<ViaLocation> viaLocations, List<JourneyRequest> viaJourneys) {
    return new Builder(new RouteViaRequest(viaLocations, viaJourneys));
  }

  public Builder copyOf() {
    return new Builder(this);
  }

  public RouteRequest routeRequest() {
    var request = new RouteRequest();

    request.setTo(to);
    request.setFrom(from);
    request.setSearchWindow(searchWindow);
    request.setDateTime(dateTime);
    request.setWheelchair(wheelchair);
    request.setPreferences(preferences);

    return request;
  }

  public List<ViaLeg> viaLegs() {
    return viaLegs;
  }

  public GenericLocation from() {
    return from;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof RouteViaRequest other)) {
      return false;
    }

    return (
      viaLegs.equals(other.viaLegs) &&
      from.equals(other.from) &&
      to.equals(other.to) &&
      dateTime.equals(other.dateTime) &&
      searchWindow.equals(other.searchWindow) &&
      wheelchair == other.wheelchair &&
      preferences.equals(other.preferences)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(viaLegs, from, to, dateTime, searchWindow, wheelchair, preferences);
  }

  public static class Builder {

    private final List<ViaLeg> viaLegs;
    private GenericLocation from;
    private GenericLocation to;
    private Instant dateTime;
    private Duration searchWindow;
    private boolean timetableView;
    private boolean arriveBy;
    private Locale locale;
    private JourneyRequest journey;
    private boolean wheelchair;
    private RoutingPreferences preferences;

    public Builder(RouteViaRequest original) {
      this.from = original.from;
      this.to = original.to;
      this.dateTime = original.dateTime;
      this.searchWindow = original.searchWindow;
      this.wheelchair = original.wheelchair;
      this.preferences = original.preferences;
      this.viaLegs = original.viaLegs;
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

    public Builder withTimetableView(boolean timetableView) {
      this.timetableView = timetableView;
      return this;
    }

    public Builder withArriveBy(boolean arriveBy) {
      this.arriveBy = arriveBy;
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

    public Builder withPreferences(Consumer<RoutingPreferences.Builder> prefs) {
      preferences = preferences.copyOf().apply(prefs).build();
      return this;
    }
  }

  public record ViaLeg(JourneyRequest journeyRequest, ViaLocation viaLocation) {}
}
