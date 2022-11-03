package org.opentripplanner.routing.api.request.request;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;

/**
 * Trip planning request with a list of via points.
 */
public class RouteViaRequest {

  private final List<ViaLeg> viaLegs = new ArrayList<>();

  private GenericLocation from;

  private GenericLocation to;

  private Instant dateTime = Instant.now();

  private Duration searchWindow;

  private boolean timetableView = true;

  private boolean arriveBy = false;

  private Locale locale = new Locale("en", "US");

  private JourneyRequest journey = new JourneyRequest();

  private boolean wheelchair = false;

  private Consumer<RoutingPreferences.Builder> preferences;

  public RouteViaRequest(List<ViaLocation> viaLocations, List<JourneyRequest> viaJourneys) {
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

  public RouteRequest routeRequest() {
    var request = new RouteRequest();

    request.setTo(to);
    request.setFrom(from);
    request.setSearchWindow(searchWindow);
    request.setDateTime(dateTime);
    request.setArriveBy(arriveBy);
    request.setLocale(locale);
    request.setJourney(journey);
    request.setWheelchair(wheelchair);
    request.setTimetableView(timetableView);

    if (preferences != null) {
      request.withPreferences(preferences);
    }

    return request;
  }

  public void withPreferences(Consumer<RoutingPreferences.Builder> preferences) {
    this.preferences = preferences;
  }

  public void setJourney(JourneyRequest journey) {
    this.journey = journey;
  }

  public JourneyRequest journey() {
    return journey;
  }

  public void setArriveBy(boolean arriveBy) {
    this.arriveBy = arriveBy;
  }

  public boolean arriveBy() {
    return arriveBy;
  }

  /**
   * Whether the trip must be wheelchair-accessible
   */
  public boolean wheelchair() {
    return wheelchair;
  }

  public void setWheelchair(boolean wheelchair) {
    this.wheelchair = wheelchair;
  }

  public Instant dateTime() {
    return dateTime;
  }

  public void setDateTime(Instant dateTime) {
    this.dateTime = dateTime;
  }

  public GenericLocation from() {
    return from;
  }

  public void setFrom(GenericLocation from) {
    this.from = from;
  }

  public GenericLocation to() {
    return to;
  }

  public void setTo(GenericLocation to) {
    this.to = to;
  }

  public Duration searchWindow() {
    return searchWindow;
  }

  public void setSearchWindow(Duration searchWindow) {
    this.searchWindow = searchWindow;
  }

  public Locale locale() {
    return locale;
  }

  public void setLocale(Locale locale) {
    this.locale = locale;
  }

  public boolean timetableView() {
    return timetableView;
  }

  public void setTimetableView(boolean timetableView) {
    this.timetableView = timetableView;
  }

  public List<ViaLeg> viaLegs() {
    return this.viaLegs;
  }

  public record ViaLeg(JourneyRequest journeyRequest, ViaLocation viaLocation) {}
}
