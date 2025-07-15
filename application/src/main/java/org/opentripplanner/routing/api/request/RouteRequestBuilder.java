package org.opentripplanner.routing.api.request;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
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
 * All defaults should be specified here in the RouteRequest, NOT as default values of parameters
 * in web services that create RouteRequests. This establishes a priority chain for default values:
 * RouteRequest field initializers, then JSON router config, then query parameters.
 */
public class RouteRequestBuilder implements Serializable {

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
  JourneyRequest journey;
  RoutingPreferences preferences;
  int numItineraries;
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
    this.journey = original.journey();
    this.preferences = original.preferences();
    this.numItineraries = original.numItineraries();
    this.defaultRequest = original.isDefaultRequest();
  }

  public RouteRequestBuilder withJourney(JourneyRequest journey) {
    this.journey = journey;
    return this;
  }

  public RouteRequestBuilder withJourney(Consumer<JourneyRequestBuilder> body) {
    withJourney(journey.copyOf().apply(body).build());
    return this;
  }

  public RouteRequestBuilder withArriveBy(boolean arriveBy) {
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

  public RouteRequestBuilder withBookingTime(Instant bookingTime) {
    this.bookingTime = bookingTime;
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
  public RouteRequestBuilder withDateTime(Instant dateTime) {
    this.dateTime = dateTime.truncatedTo(ChronoUnit.SECONDS);
    return this;
  }

  public RouteRequestBuilder withDateTime(String date, String time, ZoneId tz) {
    ZonedDateTime dateObject = DateUtils.toZonedDateTime(date, time, tz);
    withDateTime(dateObject == null ? Instant.now() : dateObject.toInstant());
    return this;
  }

  public RouteRequestBuilder withFrom(GenericLocation from) {
    this.from = from;
    return this;
  }

  public RouteRequestBuilder withTo(GenericLocation to) {
    this.to = to;
    return this;
  }

  public RouteRequestBuilder withViaLocations(final List<ViaLocation> via) {
    this.via = via;
    return this;
  }

  public RouteRequestBuilder withSearchWindow(@Nullable Duration searchWindow) {
    this.searchWindow = searchWindow;
    return this;
  }

  public RouteRequestBuilder withMaxSearchWindow(Duration maxSearchWindow) {
    this.maxSearchWindow = maxSearchWindow;
    return this;
  }

  public RouteRequestBuilder withPageCursorFromEncoded(String pageCursor) {
    this.pageCursor = PageCursor.decode(pageCursor);
    return this;
  }

  public RouteRequestBuilder withTimetableView(boolean timetableView) {
    this.timetableView = timetableView;
    return this;
  }

  public RouteRequestBuilder withNumItineraries(int numItineraries) {
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
