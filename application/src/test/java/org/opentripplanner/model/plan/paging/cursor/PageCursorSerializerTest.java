package org.opentripplanner.model.plan.paging.cursor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opentripplanner.model.plan.SortOrder.STREET_AND_DEPARTURE_TIME;
import static org.opentripplanner.model.plan.paging.cursor.PageType.PREVIOUS_PAGE;

import java.time.Duration;
import java.time.Instant;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.ItinerarySortKey;
import org.opentripplanner.utils.time.DurationUtils;

class PageCursorSerializerTest {

  private static final Instant EDT = Instant.parse("2023-12-31T23:59:59Z");
  private static final Instant LAT = Instant.parse("2024-01-15T00:00:01Z");
  private static final Duration SW = DurationUtils.duration("5h");
  private static final Instant DT = Instant.parse("2024-01-10T10:00:00Z");
  private static final Instant AT = Instant.parse("2024-01-10T12:00:00Z");
  private static final OptionalInt BSOC = OptionalInt.of(123);

  public static final String TOKEN_V1 =
    "MXxQUkVWSU9VU19QQUdFfDIwMjMtMTItMzFUMjM6NTk6NTlafDIwMjQtMDEtMTVUMDA6MDA6MDFafDVofFNUUkVFVF" +
    "9BTkRfREVQQVJUVVJFX1RJTUV8dHJ1ZXwyMDI0LTAxLTEwVDEwOjAwOjAwWnwyMDI0LTAxLTEwVDEyOjAwOjAwWnwz" +
    "fDEyMDB8";
  public static final String TOKEN_V1_W_NULLS =
    "MXxQUkVWSU9VU19QQUdFfDIwMjMtMTItMzFUMjM6NTk6NTlafHw1aHxTVFJFRVRfQU5EX0RFUEFSVFVSRV9USU1FfH" +
    "x8fHx8";
  public static final String TOKEN_V2 =
    "MnxQUkVWSU9VU19QQUdFfDIwMjMtMTItMzFUMjM6NTk6NTlafDIwMjQtMDEtMTVUMDA6MDA6MDFafDVofFNUUkVFVF" +
    "9BTkRfREVQQVJUVVJFX1RJTUV8dHJ1ZXwyMDI0LTAxLTEwVDEwOjAwOjAwWnwyMDI0LTAxLTEwVDEyOjAwOjAwWnwz" +
    "fDEyMDB8MTIzfA==";
  public static final String TOKEN_V2_W_NULLS =
    "MnxQUkVWSU9VU19QQUdFfDIwMjMtMTItMzFUMjM6NTk6NTlafHw1aHxTVFJFRVRfQU5EX0RFUEFSVFVSRV9USU1FfH" +
    "x8fHx8fA==";

  private static final ItinerarySortKey CUT = new DeduplicationPageCut(DT, AT, 1200, 3, true);

  private static final PageCursor PAGE_CURSOR_V2 = new PageCursor(
    PREVIOUS_PAGE,
    STREET_AND_DEPARTURE_TIME,
    EDT,
    LAT,
    SW,
    CUT,
    BSOC
  );

  private final PageCursor pageCursorV2withNulls = new PageCursor(
    PREVIOUS_PAGE,
    STREET_AND_DEPARTURE_TIME,
    EDT,
    null,
    SW,
    null,
    OptionalInt.empty()
  );

  @Test
  void encode() {
    assertEquals(TOKEN_V2, PageCursorSerializer.encode(PAGE_CURSOR_V2));
    assertEquals(TOKEN_V2_W_NULLS, PageCursorSerializer.encode(pageCursorV2withNulls));
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

  @Test
  void decodeTokenV2() {
    PageCursor tokenV2 = PageCursorSerializer.decode(TOKEN_V2);
    assertEquals(PREVIOUS_PAGE, tokenV2.type());
    assertEquals(STREET_AND_DEPARTURE_TIME, tokenV2.originalSortOrder());
    assertEquals(EDT, tokenV2.earliestDepartureTime());
    assertEquals(LAT, tokenV2.latestArrivalTime());
    assertEquals(SW, tokenV2.searchWindow());
    assertEquals(CUT, tokenV2.itineraryPageCut());
    assertEquals(BSOC, tokenV2.bestStreetOnlyCost());
  }

  @Test
  void decodeTokenV2_W_NULLS() {
    PageCursor tokenV2 = PageCursorSerializer.decode(TOKEN_V2_W_NULLS);
    assertEquals(PREVIOUS_PAGE, tokenV2.type());
    assertEquals(STREET_AND_DEPARTURE_TIME, tokenV2.originalSortOrder());
    assertEquals(EDT, tokenV2.earliestDepartureTime());
    assertNull(tokenV2.latestArrivalTime());
    assertEquals(SW, tokenV2.searchWindow());
    assertNull(tokenV2.itineraryPageCut());
    assertEquals(OptionalInt.empty(), tokenV2.bestStreetOnlyCost());
  }
}
