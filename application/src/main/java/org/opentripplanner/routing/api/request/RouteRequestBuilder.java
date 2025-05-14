package org.opentripplanner.routing.api.request;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.paging.cursor.PageCursor;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.routing.api.request.preference.RoutingPreferencesBuilder;
import org.opentripplanner.routing.api.request.request.JourneyRequest;
import org.opentripplanner.routing.api.request.request.JourneyRequestBuilder;
import org.opentripplanner.routing.api.request.via.ViaLocation;
import org.opentripplanner.utils.time.DateUtils;

/**
 * A trip planning request. Some parameters may not be honored by the trip planner for some or all
 * itineraries.
 * <p>
 * All defaults should be specified here in the RouteRequest, NOT as annotations on query parameters
 * in web services that create RouteRequests. This establishes a priority chain for default values:
 * RouteRequest field initializers, then JSON router config, then query parameters.
 */
public class RouteRequestBuilder implements Cloneable, Serializable {

  GenericLocation from;
  GenericLocation to;
  List<ViaLocation> via;
  Instant dateTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);
  boolean arriveBy;
  boolean timetableView;
  Duration searchWindow;
  Duration maxSearchWindow;
  Instant bookingTime;
  PageCursor pageCursor;
  boolean wheelchair;
  JourneyRequest journey;
  RoutingPreferences preferences;
  int numItineraries;
  Locale locale;
  boolean defaultRequest;

  private final RouteRequest original;

  public RouteRequestBuilder(RouteRequest original) {
    this.original = original;
    this.from = original.from();
    this.to = original.to();
    this.via = original.getViaLocations();
    this.dateTime = original.dateTime();
    this.arriveBy = original.arriveBy();
    this.timetableView = original.timetableView();
    this.searchWindow = original.searchWindow();
    this.maxSearchWindow = original.maxSearchWindow();
    this.bookingTime = original.bookingTime();
    this.pageCursor = original.pageCursor();
    this.wheelchair = original.wheelchair();
    this.journey = original.journey();
    this.preferences = original.preferences();
    this.numItineraries = original.numItineraries();
    this.locale = original.locale();
    this.defaultRequest = original.isDefaultRequest();
  }

  public RouteRequestBuilder setJourney(JourneyRequest journey) {
    this.journey = journey;
    return this;
  }

  public RouteRequestBuilder withJourney(Consumer<JourneyRequestBuilder> body) {
    setJourney(journey.copyOf().apply(body).build());
    return this;
  }

  public RouteRequestBuilder setArriveBy(boolean arriveBy) {
    this.arriveBy = arriveBy;
    return this;
  }

  public RouteRequestBuilder withPreferences(RoutingPreferences preferences) {
    this.preferences = preferences;
    return this;
  }

  public RouteRequestBuilder withPreferences(Consumer<RoutingPreferencesBuilder> body) {
    return withPreferences(preferences.copyOf().apply(body).build());
  }

  public RouteRequestBuilder setBookingTime(Instant bookingTime) {
    this.bookingTime = bookingTime;
    return this;
  }

  public RouteRequestBuilder setWheelchair(boolean wheelchair) {
    this.wheelchair = wheelchair;
    return this;
  }

  public Instant dateTime() {
    return dateTime;
  }

  /**
   * The dateTime will be set to a whole number of seconds. We don't do sub-second accuracy,
   * and if we set the millisecond part to a non-zero value, rounding will not be guaranteed
   * to be the same for departAt and arriveBy queries.
   * @param dateTime Either a departAt time or an arriveBy time, one second's accuracy
   */
  public RouteRequestBuilder setDateTime(Instant dateTime) {
    this.dateTime = dateTime.truncatedTo(ChronoUnit.SECONDS);
    return this;
  }

  public RouteRequestBuilder setDateTime(String date, String time, ZoneId tz) {
    ZonedDateTime dateObject = DateUtils.toZonedDateTime(date, time, tz);
    setDateTime(dateObject == null ? Instant.now() : dateObject.toInstant());
    return this;
  }

  public RouteRequestBuilder setFrom(GenericLocation from) {
    this.from = from;
    return this;
  }

  public RouteRequestBuilder setTo(GenericLocation to) {
    this.to = to;
    return this;
  }

  public RouteRequestBuilder setViaLocations(final List<ViaLocation> via) {
    this.via = via;
    return this;
  }

  public RouteRequestBuilder setSearchWindow(@Nullable Duration searchWindow) {
    this.searchWindow = searchWindow;
    return this;
  }

  public RouteRequestBuilder setMaxSearchWindow(Duration maxSearchWindow) {
    this.maxSearchWindow = maxSearchWindow;
    return this;
  }

  public RouteRequestBuilder setLocale(Locale locale) {
    this.locale = locale;
    return this;
  }

  public RouteRequestBuilder setPageCursorFromEncoded(String pageCursor) {
    this.pageCursor = PageCursor.decode(pageCursor);
    return this;
  }

  public RouteRequestBuilder setTimetableView(boolean timetableView) {
    this.timetableView = timetableView;
    return this;
  }

  public RouteRequestBuilder setNumItineraries(int numItineraries) {
    this.numItineraries = numItineraries;
    return this;
  }

  public RouteRequest buildDefault() {
    if (!defaultRequest) {
      throw new IllegalStateException(
        "A default request can only be created based on another default request!"
      );
    }
    return build();
  }

  public RouteRequest buildRequest() {
    this.defaultRequest = false;
    return build();
  }

  private RouteRequest build() {
    var value = new RouteRequest(this);
    return original.equals(value) ? original : value;
  }
}
