package org.opentripplanner.model.plan.paging.cursor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.model.plan.SortOrder.STREET_AND_DEPARTURE_TIME;
import static org.opentripplanner.model.plan.paging.cursor.PageType.PREVIOUS_PAGE;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.model.plan.ItinerarySortKey;
import org.opentripplanner.utils.time.DurationUtils;

class PageCursorSerializerTest {

  private static final Instant EDT = Instant.parse("2023-12-31T23:59:59Z");
  private static final Instant LAT = Instant.parse("2024-01-15T00:00:01Z");
  private static final Duration SW = DurationUtils.duration("5h");
  private static final Instant DT = Instant.parse("2024-01-10T10:00:00Z");
  private static final Instant AT = Instant.parse("2024-01-10T12:00:00Z");
  private static final Cost GENERALIZED_COST_MAX_LIMIT = Cost.costOfSeconds(177);

  private static final String TOKEN_V1 = "MX";
  private static final String TOKEN_V2 = "Mn";
  private static final String TOKEN_BODY =
    "xQUkVWSU9VU19QQUdFfDIwMjMtMTItMzFUMjM6NTk6NTlafDIwMjQtMDEtMTVUMDA6MDA6MDFafDVofFNUUkVFVF" +
    "9BTkRfREVQQVJUVVJFX1RJTUV8dHJ1ZXwyMDI0LTAxLTEwVDEwOjAwOjAwWnwyMDI0LTAxLTEwVDEyOjAwOjAwWn" +
    "wzfDEyMDB8";

  private static final String TOKEN_W_NULLS_BODY =
    "xQUkVWSU9VU19QQUdFfDIwMjMtMTItMzFUMjM6NTk6NTlafHw1aHxTVFJFRVRfQU5EX0RFUEFSVFVSRV9USU1FfH" +
    "x8fHx8";

  private static final String PREV_TOKEN = TOKEN_V1 + TOKEN_BODY;
  private static final String CURR_TOKEN = TOKEN_V2 + TOKEN_BODY + "MTc3fA==";
  private static final String PREV_TOKEN_W_NULLS = TOKEN_V1 + TOKEN_W_NULLS_BODY + "";
  private static final String CURR_TOKEN_W_NULLS = TOKEN_V2 + TOKEN_W_NULLS_BODY + "fA==";

  private static final ItinerarySortKey CUT = new DeduplicationPageCut(
    DT,
    AT,
    Cost.costOfSeconds(1200),
    3,
    true
  );

  private static final PageCursor PAGE_CURSOR = new PageCursor(
    PREVIOUS_PAGE,
    STREET_AND_DEPARTURE_TIME,
    EDT,
    LAT,
    SW,
    CUT,
    GENERALIZED_COST_MAX_LIMIT
  );

  private static final PageCursor PAGE_CURSOR_WITH_NULLS = new PageCursor(
    PREVIOUS_PAGE,
    STREET_AND_DEPARTURE_TIME,
    EDT,
    null,
    SW,
    null,
    null
  );

  @Test
  void encode() {
    String resultOne = PageCursorSerializer.encode(PAGE_CURSOR);

    assertEquals(CURR_TOKEN, resultOne);
    String resultTwo = PageCursorSerializer.encode(PAGE_CURSOR_WITH_NULLS);
    assertEquals(CURR_TOKEN_W_NULLS, resultTwo);

    System.out.println(resultOne);
    System.out.println(resultTwo);
  }

  @Test
  void decodeTokenCurrentVersion() {
    var token = PageCursorSerializer.decode(CURR_TOKEN);
    assertEquals(PREVIOUS_PAGE, token.type());
    assertEquals(STREET_AND_DEPARTURE_TIME, token.originalSortOrder());
    assertEquals(EDT, token.earliestDepartureTime());
    assertEquals(LAT, token.latestArrivalTime());
    assertEquals(SW, token.searchWindow());
    assertTrue(token.containsItineraryPageCut());
    assertEquals(CUT, token.itineraryPageCut());
    assertTrue(token.containsGeneralizedCostMaxLimit());
    assertEquals(GENERALIZED_COST_MAX_LIMIT, token.generalizedCostMaxLimit());
  }

  @Test
  void decodeTokenPreviousVersion() {
    var token = PageCursorSerializer.decode(PREV_TOKEN);
    assertEquals(PREVIOUS_PAGE, token.type());
    assertEquals(STREET_AND_DEPARTURE_TIME, token.originalSortOrder());
    assertEquals(EDT, token.earliestDepartureTime());
    assertEquals(LAT, token.latestArrivalTime());
    assertEquals(SW, token.searchWindow());
    assertTrue(token.containsItineraryPageCut());
    assertEquals(CUT, token.itineraryPageCut());
    assertFalse(token.containsGeneralizedCostMaxLimit());
    assertNull(token.generalizedCostMaxLimit());
  }

  @Test
  void decodeTokenCurrentVersionWithNulls() {
    var token = PageCursorSerializer.decode(CURR_TOKEN_W_NULLS);
    assertEquals(PREVIOUS_PAGE, token.type());
    assertEquals(STREET_AND_DEPARTURE_TIME, token.originalSortOrder());
    assertEquals(EDT, token.earliestDepartureTime());
    assertNull(token.latestArrivalTime());
    assertEquals(SW, token.searchWindow());
    assertFalse(token.containsItineraryPageCut());
    assertNull(token.itineraryPageCut());
    assertFalse(token.containsGeneralizedCostMaxLimit());
    assertNull(token.generalizedCostMaxLimit());
  }

  @Test
  void decodeTokenPreviousVersionWithNulls() {
    var token = PageCursorSerializer.decode(PREV_TOKEN_W_NULLS);
    assertEquals(PREVIOUS_PAGE, token.type());
    assertEquals(STREET_AND_DEPARTURE_TIME, token.originalSortOrder());
    assertEquals(EDT, token.earliestDepartureTime());
    assertNull(token.latestArrivalTime());
    assertEquals(SW, token.searchWindow());
    assertFalse(token.containsItineraryPageCut());
    assertNull(token.itineraryPageCut());
    assertFalse(token.containsGeneralizedCostMaxLimit());
    assertNull(token.generalizedCostMaxLimit());
  }
}
