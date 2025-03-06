package org.opentripplanner.model.plan.paging.cursor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opentripplanner.model.plan.SortOrder.STREET_AND_ARRIVAL_TIME;
import static org.opentripplanner.model.plan.SortOrder.STREET_AND_DEPARTURE_TIME;
import static org.opentripplanner.model.plan.paging.cursor.PageType.NEXT_PAGE;
import static org.opentripplanner.model.plan.paging.cursor.PageType.PREVIOUS_PAGE;
import static org.opentripplanner.utils.collection.ListSection.HEAD;
import static org.opentripplanner.utils.collection.ListSection.TAIL;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.TimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.model.plan.SortOrder;
import org.opentripplanner.model.plan.TestItineraryBuilder;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.utils.collection.ListSection;

class PageCursorTest implements PlanTestConstants {

  public static final TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();
  public static final Place A = Place.forStop(
    TEST_MODEL.stop("A").withCoordinate(5.0, 8.0).build()
  );
  public static final Place B = Place.forStop(
    TEST_MODEL.stop("B").withCoordinate(6.0, 8.5).build()
  );

  private static final ZoneId ZONE_ID = ZoneIds.GMT;
  private static final String EDT_STR = "2021-01-31T12:20:00Z";
  private static final String LAT_STR = "2021-01-31T15:00:00Z";
  private static final Instant EDT = Instant.parse(EDT_STR);
  private static final Instant LAT = Instant.parse(LAT_STR);
  private static final Duration SEARCH_WINDOW = Duration.parse("PT2h");
  private static final Itinerary PAGE_CUT = TestItineraryBuilder.newItinerary(A, 0)
    .walk(20, Place.forStop(TEST_MODEL.stop("1:stop", 1d, 1d).build()))
    .bus(23, 0, 50, B)
    .build();

  private TimeZone originalTimeZone;
  private PageCursor subjectDepartAfter;
  private PageCursor subjectArriveBy;

  @BeforeEach
  public void setup() {
    originalTimeZone = TimeZone.getDefault();
    TimeZone.setDefault(TimeZone.getTimeZone(ZONE_ID));

    subjectDepartAfter = new PageCursor(
      NEXT_PAGE,
      STREET_AND_ARRIVAL_TIME,
      EDT,
      null,
      SEARCH_WINDOW,
      null
    );
    subjectArriveBy = new PageCursor(
      PREVIOUS_PAGE,
      STREET_AND_DEPARTURE_TIME,
      EDT,
      LAT,
      SEARCH_WINDOW,
      PAGE_CUT
    );
  }

  @AfterEach
  public void teardown() {
    TimeZone.setDefault(originalTimeZone);
  }

  @Test
  public void testToString() {
    assertEquals(
      "PageCursor{type: NEXT_PAGE, sortOrder: STREET_AND_ARRIVAL_TIME, " +
      "edt: " +
      EDT_STR +
      ", searchWindow: 2h}",
      subjectDepartAfter.toString()
    );
    assertEquals(
      "PageCursor{type: PREVIOUS_PAGE, sortOrder: STREET_AND_DEPARTURE_TIME, " +
      "edt: " +
      EDT_STR +
      ", lat: " +
      LAT_STR +
      ", searchWindow: 2h, " +
      "itineraryPageCut: [2020-02-02T00:00:00Z, 2020-02-02T00:00:50Z, $194, Tx0, transit]}",
      subjectArriveBy.toString()
    );
  }

  static List<Arguments> cropItinerariesAtTestCase() {
    return List.of(
      Arguments.of(NEXT_PAGE, STREET_AND_ARRIVAL_TIME, TAIL),
      Arguments.of(NEXT_PAGE, STREET_AND_DEPARTURE_TIME, HEAD),
      Arguments.of(PREVIOUS_PAGE, STREET_AND_ARRIVAL_TIME, HEAD),
      Arguments.of(PREVIOUS_PAGE, STREET_AND_DEPARTURE_TIME, TAIL)
    );
  }

  @ParameterizedTest
  @MethodSource("cropItinerariesAtTestCase")
  public void cropItinerariesAt(PageType page, SortOrder order, ListSection expSection) {
    assertEquals(
      expSection,
      new PageCursor(page, order, EDT, null, SEARCH_WINDOW, null).cropItinerariesAt()
    );
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void encodeAndDecode() {
    // depart after
    String buf = subjectDepartAfter.encode();
    var before = PageCursor.decode(buf);
    assertEquals(subjectDepartAfter.toString(), before.toString());

    // Arrive by
    buf = subjectArriveBy.encode();
    before = PageCursor.decode(buf);
    assertEquals(subjectArriveBy.toString(), before.toString());
  }

  @Test
  public void testDecodeEmptyCursor() {
    assertNull(PageCursor.decode(null));
    assertNull(PageCursor.decode(""));
    assertNull(PageCursor.decode(" "));
    assertNull(PageCursor.decode("null"));
    assertNull(PageCursor.decode("09#$%+1~^§€"));
  }
}
