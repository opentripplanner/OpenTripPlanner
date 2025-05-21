package org.opentripplanner.service.paging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.paging.cursor.PageType.NEXT_PAGE;
import static org.opentripplanner.model.plan.paging.cursor.PageType.PREVIOUS_PAGE;
import static org.opentripplanner.service.paging.TestPagingUtils.cleanStr;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.ItinerarySortKey;
import org.opentripplanner.model.plan.SortOrder;
import org.opentripplanner.model.plan.paging.cursor.PageCursor;
import org.opentripplanner.model.plan.paging.cursor.PageType;
import org.opentripplanner.utils.time.TimeUtils;

/**
 * This test focus on testing the paging with few itineraries. There should be no page-cuts. The
 * test focus on paging back and forth, matching itineraries with departure time at
 * the exact same time as the search-window earliest-departure-time.
 * <p>
 * Note! We are not doing the actual search, just emulating the search using the
 * {@link TestDriver} mock.
 * <p>
 * All components required to test paging is used including the {@link PagingService} and the
 * 3 filters:
 * <ol>
 *   <li>PagingFilter</li>
 *   <li>OutsideSearchWindowFilter</li>
 *   <li>NumItinerariesFilter</li>
 * </ol>
 * <p>
 */
@SuppressWarnings("DataFlowIssue")
class PS3_FewItinerariesOnSearchWindowLimitTest {

  private static final Duration SEARCH_WINDOW = Duration.ofHours(6);
  private static final int SEARCH_WINDOW_SEC = (int) SEARCH_WINDOW.toSeconds();
  /** We are avoiding the pageCut in this test - hence setting the value high > 2*/
  private static final int NUM_OF_ITINERARIES = 10;
  private static final String LATEST_ARRIVAL_TIME_TEXT = "12:00+1d";
  private static final int LATEST_ARRIVAL_TIME = TimeUtils.time(LATEST_ARRIVAL_TIME_TEXT);
  private static final boolean DEPART_AFTER = false;
  private static final boolean ARRIVE_BY = true;
  public static final String EMPTY = "";

  private final TestPagingModel model = TestPagingModel.testDataWithFewItinerariesCaseB();
  private TestDriver driver;

  /**
   * List of test-cases - for example:
   * <pre>
   *   Given: "08:00", DEPART_AFTER, "1 N 2 N - P 2 -"
   *     - "08:00"        : The first search departure time
   *     - DEPART_AFTER   : If first search is arriveBy or depart after search
   *     - "1 N 2 N - .." : Paging sequence - N:NEXT or P:PREVIOUS, 0-4 is the expected itinerary
   *                        index found in the search between paging events, and '-' means no
   *                        itinerary found.
   * </pre>
   */
  static List<Arguments> testCases() {
    return List.of(
      // Itineraries depart inside search-window, step NEXT 6 times
      //Arguments.of("08:00", DEPART_AFTER, "1 N 2 N - N - N 3 N - N"),
      // Itineraries depart inside search-window, step PREV 6 times
      //Arguments.of("08:00+1d", DEPART_AFTER, "3 P - P - P 2 P 1 P - P"),
      // Itineraries depart inside search-window, step BACK and FORTH
      //Arguments.of("08:00", DEPART_AFTER, "1 N 2 N - P 2 N - P 2 P 1 N 2 N"),
      // Itineraries depart exactly at the start of search-window [inc, exc>, same sequence as
      // test above
      Arguments.of("09:00", DEPART_AFTER, "1 N 2 N - P 2 N - P 2 P 1 N 2 N"),
      // Itineraries depart inside search-window, step NEXT 6 times
      Arguments.of("10:00-1d", ARRIVE_BY, "3 N - N - N 2 N 1 N - N"),
      // Itineraries depart inside search-window, step PREV 6 times
      Arguments.of("08:00+1d", ARRIVE_BY, "0 P - P - P 1 P 2 P - P"),
      // Itineraries depart inside search-window, step BACK and FORTH
      Arguments.of("08:00", ARRIVE_BY, "2 P - P - P 3 N - N - P - N - N 2 N 1 P 2"),
      // Itineraries depart exactly at the start of search-window [inc, exc>, same sequence as
      // test above
      Arguments.of("09:00", ARRIVE_BY, "2 P - P - P 3 N - N - P - N - N 2 N 1 P 2")
    );
  }

  /**
   * Test paging DEPART AFTER search with NEXT -> NEXT/PREVIOUS
   */
  @ParameterizedTest
  @MethodSource("testCases")
  void test(String edt, boolean arriveBy, String testCaseTokens) {
    PageCursor cursor;
    SortOrder expectedSortOrder;

    int currTime = TimeUtils.time(edt);

    if (arriveBy) {
      expectedSortOrder = SortOrder.STREET_AND_DEPARTURE_TIME;
      driver = model.arriveByDriver(
        currTime,
        LATEST_ARRIVAL_TIME,
        SEARCH_WINDOW,
        NUM_OF_ITINERARIES
      );
    } else {
      expectedSortOrder = SortOrder.STREET_AND_ARRIVAL_TIME;
      driver = model.departAfterDriver(currTime, SEARCH_WINDOW, NUM_OF_ITINERARIES);
    }

    var subject = driver.pagingService();
    var testCases = parseTestCases(testCaseTokens);

    for (var tc : testCases) {
      if (tc.gotoNewPage()) {
        if (tc.gotoPage() == NEXT_PAGE) {
          cursor = subject.nextPageCursor();
          currTime += SEARCH_WINDOW_SEC;
        } else {
          cursor = subject.previousPageCursor();
          currTime -= SEARCH_WINDOW_SEC;
        }
        String description = tc.testDescription();
        assertEquals(tc.gotoPage(), cursor.type(), description);
        assertEquals(expectedSortOrder, cursor.originalSortOrder(), description);
        assertEquals(SEARCH_WINDOW, cursor.searchWindow(), description);
        assertEquals(
          TimeUtils.timeToStrCompact(currTime),
          cleanStr(cursor.earliestDepartureTime()),
          description
        );

        // Expect initial LAT - could not change to another time - TODO: FIX THIS AND ENABLE TEST
        // assertEquals(expLat, cleanStr(cursor.earliestDepartureTime()), description);
        driver = driver.newPage(cursor);
        subject = driver.pagingService(cursor);
      } else {
        assertEquals(tc.expItinerary, getResultAsString(driver.kept()), tc.testDescription());
      }
    }
  }

  private List<TestCase> parseTestCases(String tokensAsText) {
    var tokens = tokensAsText.split("\\s+");
    var sequence = new StringBuilder();
    return Arrays.stream(tokens).map(token -> parseToken(token, sequence)).toList();
  }

  private TestCase parseToken(String token, StringBuilder sequence) {
    switch (token) {
      case "-":
        sequence.append(" > -");
        return new TestCase(sequence.substring(3), null, -1, EMPTY);
      case "0", "1", "2", "3":
        sequence.append(" > ").append(token);
        int i = Integer.parseInt(token);
        return new TestCase(
          sequence.substring(3),
          null,
          i,
          cleanStr(driver.all().get(i).keyAsString())
        );
      case "N":
        sequence.append(" > NEXT");
        return new TestCase(sequence.substring(3), NEXT_PAGE, -1, EMPTY);
      case "P":
        sequence.append(" > PREV");
        return new TestCase(sequence.substring(3), PREVIOUS_PAGE, -1, EMPTY);
      default:
        throw new IllegalArgumentException(token);
    }
  }

  private static String getResultAsString(List<Itinerary> kept) {
    return kept
      .stream()
      .map(ItinerarySortKey::keyAsString)
      .map(TestPagingUtils::cleanStr)
      .collect(Collectors.joining());
  }

  record TestCase(String sequence, PageType gotoPage, int expIndex, String expItinerary) {
    boolean gotoNewPage() {
      return gotoPage != null;
    }

    String testDescription() {
      return "Failed after paging sequence: ( " + sequence + " )";
    }
  }
}
