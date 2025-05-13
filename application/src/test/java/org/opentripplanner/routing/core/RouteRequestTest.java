package org.opentripplanner.routing.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opentripplanner._support.asserts.AssertString.assertEqualsIgnoreWhitespace;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.asserts.AssertEqualsAndHashCode;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.SortOrder;
import org.opentripplanner.model.plan.paging.cursor.PageCursor;
import org.opentripplanner.model.plan.paging.cursor.PageType;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.routing.api.request.request.JourneyRequest;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.api.request.via.ViaLocation;
import org.opentripplanner.routing.api.request.via.VisitViaLocation;
import org.opentripplanner.routing.api.response.InputField;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingErrorCode;
import org.opentripplanner.routing.error.RoutingValidationException;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class RouteRequestTest {

  private static final GenericLocation FROM = GenericLocation.fromCoordinate(60.0, 10.0);
  private static final GenericLocation TO = GenericLocation.fromCoordinate(59.0, 12.0);
  private static final List<ViaLocation> VIA = List.of(
    new VisitViaLocation(
      "Via",
      Duration.ofMinutes(10),
      List.of(),
      List.of(new WgsCoordinate(59.5, 11.0))
    )
  );
  private static final Instant DATE_TIME = LocalDateTime.of(2025, Month.MAY, 17, 11, 15).toInstant(
    ZoneOffset.UTC
  );
  private static final boolean ARRIVE_BY = true;
  private static final boolean TIMETABLE_VIEW = false;
  private static final Duration SEARCH_WINDOW = Duration.ofHours(2);
  private static final Duration MAX_SEARCH_WINDOW = Duration.ofHours(36);
  private static final Instant BOOKING_TIME = LocalDateTime.of(
    2025,
    Month.MAY,
    17,
    11,
    15
  ).toInstant(ZoneOffset.UTC);
  private static final PageCursor PAGE_CURSOR = new PageCursor(
    PageType.NEXT_PAGE,
    SortOrder.STREET_AND_ARRIVAL_TIME,
    DATE_TIME.plusSeconds(60 * 12),
    null,
    SEARCH_WINDOW,
    null,
    null
  );
  private static final boolean WHEELCHAIR = true;
  private static final JourneyRequest JOURNEY = JourneyRequest.of()
    .withAccess(new StreetRequest(StreetMode.BIKE))
    .build();
  private static final RoutingPreferences PREFERENCES = RoutingPreferences.of()
    .withTransfer(tb -> tb.withMaxTransfers(10))
    .build();
  private static final int NUM_ITINERARIES = 10;
  private static final Locale LOCALE = Locale.FRENCH;

  private static final Duration DURATION_24_HOURS = Duration.ofHours(24);
  private static final Duration DURATION_ZERO = Duration.ofMinutes(0);
  private static final Duration DURATION_ONE_MINUTE = Duration.ofMinutes(1);
  private static final Duration DURATION_MINUS_ONE_MINUTE = DURATION_ONE_MINUTE.negated();

  private final RouteRequest subject = RouteRequest.of()
    .setFrom(FROM)
    .setTo(TO)
    .setViaLocations(VIA)
    .setDateTime(DATE_TIME)
    .setArriveBy(ARRIVE_BY)
    .setTimetableView(TIMETABLE_VIEW)
    .setSearchWindow(SEARCH_WINDOW)
    .setMaxSearchWindow(MAX_SEARCH_WINDOW)
    .setBookingTime(BOOKING_TIME)
    .setPageCursorFromEncoded(PAGE_CURSOR.encode())
    .setWheelchair(WHEELCHAIR)
    .setJourney(JOURNEY)
    .withPreferences(PREFERENCES)
    .setNumItineraries(NUM_ITINERARIES)
    .setLocale(LOCALE)
    .buildRequest();

  private final RouteRequest minimal = RouteRequest.of()
    .setFrom(FROM)
    .setTo(TO)
    .setDateTime(DATE_TIME)
    .buildRequest();

  @Test
  void from() {
    assertEquals(FROM, subject.from());
  }

  @Test
  void to() {
    assertEquals(TO, subject.to());
  }

  @Test
  void via() {
    assertEquals(VIA, subject.getViaLocations());
  }

  @Test
  void dateTime() {
    assertEquals(DATE_TIME, subject.dateTime());
  }

  @Test
  void arriveBy() {
    assertEquals(ARRIVE_BY, subject.arriveBy());
  }

  @Test
  void timetableView() {
    assertEquals(TIMETABLE_VIEW, subject.timetableView());
  }

  @Test
  void searchWindow() {
    assertEquals(SEARCH_WINDOW, subject.searchWindow());
  }

  @Test
  void maxSearchWindow() {
    assertEquals(MAX_SEARCH_WINDOW, subject.maxSearchWindow());
  }

  @Test
  void bookingTime() {
    assertEquals(BOOKING_TIME, subject.bookingTime());
  }

  @Test
  void wheelchair() {
    assertEquals(WHEELCHAIR, subject.wheelchair());
  }

  @Test
  void journey() {
    assertEquals(JOURNEY, subject.journey());
  }

  @Test
  void preferences() {
    assertEquals(PREFERENCES, subject.preferences());
  }

  @Test
  void numItineraries() {
    assertEquals(NUM_ITINERARIES, subject.numItineraries());
  }

  @Test
  void locale() {
    assertEquals(LOCALE, subject.locale());
  }

  @Test
  void testBuildDefault() {
    // Allow to build request without from, to, and dateTime
    var subject = RouteRequest.of()
      .setNumItineraries(NUM_ITINERARIES)
      .setSearchWindow(SEARCH_WINDOW)
      .buildDefault();
    assertEquals("RouteRequest{searchWindow: 2h, numItineraries: 10}", subject.toString());

    assertThrows(IllegalStateException.class, () -> RouteRequest.of().setFrom(FROM).buildDefault());
    assertThrows(IllegalStateException.class, () -> RouteRequest.of().setTo(TO).buildDefault());
    assertThrows(IllegalStateException.class, () ->
      RouteRequest.of().setDateTime(DATE_TIME).buildDefault()
    );
    assertThrows(IllegalStateException.class, () -> minimal.copyOf().buildDefault());
  }

  @Test
  void testEqualsAndHashCode() {
    // Change subject and change back to force a new instance to be created.
    var same = subject
      .copyOf()
      .setArriveBy(!ARRIVE_BY)
      .buildRequest()
      .copyOf()
      .setArriveBy(ARRIVE_BY)
      .buildRequest();

    AssertEqualsAndHashCode.verify(subject)
      .sameAs(same)
      .differentFrom(
        subject.copyOf().setFrom(TO).buildRequest(),
        subject.copyOf().setTo(FROM).buildRequest(),
        subject.copyOf().setDateTime(Instant.now()).buildRequest(),
        subject.copyOf().setArriveBy(!ARRIVE_BY).buildRequest(),
        subject.copyOf().setTimetableView(!TIMETABLE_VIEW).buildRequest(),
        subject.copyOf().setSearchWindow(SEARCH_WINDOW.plusSeconds(10)).buildRequest(),
        subject.copyOf().setMaxSearchWindow(MAX_SEARCH_WINDOW.plusHours(1)).buildRequest(),
        subject.copyOf().setBookingTime(BOOKING_TIME.plusSeconds(10)).buildRequest(),
        subject.copyOf().setPageCursorFromEncoded(null).buildRequest(),
        subject.copyOf().setWheelchair(!WHEELCHAIR).buildRequest(),
        subject.copyOf().setJourney(JourneyRequest.DEFAULT).buildRequest(),
        subject.copyOf().withPreferences(RoutingPreferences.DEFAULT).buildRequest()
      );
  }

  @Test
  void testToString() {
    assertEqualsIgnoreWhitespace("RouteRequest{}", RouteRequest.defaultValue().toString());

    assertEquals(
      "RouteRequest{from: (60.0, 10.0), to: (59.0, 12.0), dateTime: 2025-05-17T11:15:00Z}",
      minimal.toString()
    );

    assertEqualsIgnoreWhitespace(
      """
      RouteRequest{
        from:(60.0,10.0),
        to:(59.0,12.0),
        via: [VisitViaLocation{label:Via,minimumWaitTime:10m,coordinates:[(59.5,11.0)]}],
        dateTime:2025-05-17T11:15:00Z,
        arriveBy,
        timetableView:false,
        searchWindow:2h,
        maxSearchWindow:1d12h,
        bookingTime:2025-05-17T11:15:00Z,
        numItineraries:10,
        wheelchair,
        locale:fr,
        preferences:RoutingPreferences{transfer:TransferPreferences{maxTransfers:10}},
        journey:JourneyRequest{access:StreetRequest{mode:BIKE}}
      }
      """,
      subject.toString()
    );
  }

  @Test
  void testValidateMissingFrom() {
    expectOneRoutingValidationException(
      () -> minimal.copyOf().setFrom(GenericLocation.UNKNOWN).buildRequest(),
      RoutingErrorCode.LOCATION_NOT_FOUND,
      InputField.FROM_PLACE
    );
  }

  @Test
  void testValidateMissingTo() {
    expectOneRoutingValidationException(
      () -> minimal.copyOf().setTo(GenericLocation.UNKNOWN).buildRequest(),
      RoutingErrorCode.LOCATION_NOT_FOUND,
      InputField.TO_PLACE
    );
  }

  @Test
  void testZeroSearchWindowIsAlowed() {
    // no excaption thrown when search-window is 0s
    minimal.copyOf().setSearchWindow(DURATION_ZERO).buildRequest();
  }

  @Test
  void testTooLongSearchWindow() {
    var ex = assertThrows(IllegalArgumentException.class, () ->
      minimal
        .copyOf()
        .setMaxSearchWindow(DURATION_24_HOURS)
        .setSearchWindow(DURATION_24_HOURS.plusMinutes(1))
        .buildRequest()
    );
    assertEquals("The search window cannot exceed PT24H", ex.getMessage());
  }

  @Test
  void testNegativeSearchWindow() {
    var ex = assertThrows(IllegalArgumentException.class, () ->
      minimal.copyOf().setSearchWindow(Duration.ofSeconds(-1)).buildRequest()
    );
    assertEquals("The search window must be a positive duration", ex.getMessage());
  }

  @Test
  void allowTransferOptimization() {
    RouteRequest request = new RouteRequest();
    assertTrue(request.allowTransferOptimization());

    request.setViaLocations(
      List.of(new VisitViaLocation("VIA", null, List.of(new FeedScopedId("F", "1")), List.of()))
    );
    assertFalse(request.allowTransferOptimization());
  }

  private void expectOneRoutingValidationException(
    Runnable body,
    RoutingErrorCode expCode,
    InputField expField
  ) {
    try {
      body.run();
      fail();
    } catch (RoutingValidationException rve) {
      List<RoutingError> errors = rve.getRoutingErrors();
      assertEquals(1, errors.size(), () -> errors.toString());
      assertTrue(errors.stream().anyMatch(e -> e.code == expCode && e.inputField == expField));
    }
  }
}
