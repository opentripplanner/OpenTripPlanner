package org.opentripplanner.model.plan.paging.cursor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opentripplanner.model.plan.SortOrder.STREET_AND_DEPARTURE_TIME;
import static org.opentripplanner.model.plan.paging.cursor.PageType.PREVIOUS_PAGE;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.time.DurationUtils;
import org.opentripplanner.model.plan.ItinerarySortKey;

class PageCursorSerializerTest {

  private static final Instant EDT = Instant.parse("2023-12-31T23:59:59Z");
  private static final Instant LAT = Instant.parse("2024-01-15T00:00:01Z");
  private static final Duration SW = DurationUtils.duration("5h");
  private static final Instant DT = Instant.parse("2024-01-10T10:00:00Z");
  private static final Instant AT = Instant.parse("2024-01-10T12:00:00Z");

  public static final String TOKEN_V1 =
    "MXxQUkVWSU9VU19QQUdFfDIwMjMtMTItMzFUMjM6NTk6NTlafDIwMjQtMDEtMTVUMDA6MDA6MDFafDVofFNUUkVFVF" +
    "9BTkRfREVQQVJUVVJFX1RJTUV8dHJ1ZXwyMDI0LTAxLTEwVDEwOjAwOjAwWnwyMDI0LTAxLTEwVDEyOjAwOjAwWnwz" +
    "fDEyMDB8";
  public static final String TOKEN_V1_W_NULLS =
    "MXxQUkVWSU9VU19QQUdFfDIwMjMtMTItMzFUMjM6NTk6NTlafHw1aHxTVFJFRVRfQU5EX0RFUEFSVFVSRV9USU1FfH" +
    "x8fHx8";

  private static final ItinerarySortKey CUT = new DeduplicationPageCut(DT, AT, 1200, 3, true);

  private static final PageCursor PAGE_CURSOR_V1 = new PageCursor(
    PREVIOUS_PAGE,
    STREET_AND_DEPARTURE_TIME,
    EDT,
    LAT,
    SW,
    CUT
  );

  private final PageCursor pageCursorV1withNulls = new PageCursor(
    PREVIOUS_PAGE,
    STREET_AND_DEPARTURE_TIME,
    EDT,
    null,
    SW,
    null
  );

  @Test
  void encode() {
    assertEquals(TOKEN_V1, PageCursorSerializer.encode(PAGE_CURSOR_V1));
    assertEquals(TOKEN_V1_W_NULLS, PageCursorSerializer.encode(pageCursorV1withNulls));
  }

  @Test
  void decodeTokenV1() {
    PageCursor tokenV1 = PageCursorSerializer.decode(TOKEN_V1);
    assertEquals(PREVIOUS_PAGE, tokenV1.type());
    assertEquals(STREET_AND_DEPARTURE_TIME, tokenV1.originalSortOrder());
    assertEquals(EDT, tokenV1.earliestDepartureTime());
    assertEquals(LAT, tokenV1.latestArrivalTime());
    assertEquals(SW, tokenV1.searchWindow());
    assertEquals(CUT, tokenV1.itineraryPageCut());
  }

  @Test
  void decodeTokenV1_W_NULLS() {
    PageCursor tokenV1 = PageCursorSerializer.decode(TOKEN_V1_W_NULLS);
    assertEquals(PREVIOUS_PAGE, tokenV1.type());
    assertEquals(STREET_AND_DEPARTURE_TIME, tokenV1.originalSortOrder());
    assertEquals(EDT, tokenV1.earliestDepartureTime());
    assertNull(tokenV1.latestArrivalTime());
    assertEquals(SW, tokenV1.searchWindow());
    assertNull(tokenV1.itineraryPageCut());
  }
}
