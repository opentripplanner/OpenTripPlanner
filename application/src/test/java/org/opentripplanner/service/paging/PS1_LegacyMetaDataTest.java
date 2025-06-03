package org.opentripplanner.service.paging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.service.paging.TestPagingModel.D30m;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.opentripplanner.utils.time.TimeUtils;

/**
 * This tests the entire paging service module.
 * <p>
 * To debug this test, set either the system property 'testDebug' or the environment variable 'testDebug' to 'true'.
 */
@SuppressWarnings("DataFlowIssue")
class PS1_LegacyMetaDataTest {

  private static final int T08_45 = TimeUtils.time("08:45");
  private static final int T09_15 = TimeUtils.time("09:15");

  private static final int T12_00 = TimeUtils.time("12:00");
  private static final int T13_00 = TimeUtils.time("13:00");

  private static final Duration SEARCH_WINDOW_USED = D30m;

  @Test
  @SuppressWarnings("deprecation")
  void testCreateTripSearchMetadataDepartAfterWithPageCut() {
    var model = TestPagingModel.testDataWithManyItinerariesCaseA();
    var testDriver = model.departAfterDriver(T12_00, SEARCH_WINDOW_USED, 3);
    var subject = testDriver.pagingService();

    assertEquals(D30m, subject.createTripSearchMetadata().searchWindowUsed);
    assertEquals(
      "11:30",
      TestPagingUtils.cleanStr(subject.createTripSearchMetadata().prevDateTime)
    );
    // 12:11 will drop results, the solution is to use the complete sort-vector.
    // The cursor implementation does that
    assertEquals(
      "12:11",
      TestPagingUtils.cleanStr(subject.createTripSearchMetadata().nextDateTime)
    );
  }

  @Test
  @SuppressWarnings("deprecation")
  void testCreateTripSearchMetadataDepartAfterNormalSearchWindow() {
    var model = TestPagingModel.testDataWithFewItinerariesCaseB();
    var testDriver = model.departAfterDriver(T08_45, SEARCH_WINDOW_USED, 3);
    var subject = testDriver.pagingService();

    assertEquals(D30m, subject.createTripSearchMetadata().searchWindowUsed);
    assertEquals("8:15", TestPagingUtils.cleanStr(subject.createTripSearchMetadata().prevDateTime));
    // 12:11 will drop results, the solution is to use the complete sort-vector.
    // The cursor implementation does that
    assertEquals("9:15", TestPagingUtils.cleanStr(subject.createTripSearchMetadata().nextDateTime));
  }

  @Test
  @SuppressWarnings("deprecation")
  void testCreateTripSearchMetadataArriveByWithPageCut() {
    var model = TestPagingModel.testDataWithManyItinerariesCaseA();
    var testDriver = model.arriveByDriver(T12_00, T13_00, SEARCH_WINDOW_USED, 3);
    var subject = testDriver.pagingService();

    assertEquals(D30m, subject.createTripSearchMetadata().searchWindowUsed);
    assertEquals(
      "11:39",
      TestPagingUtils.cleanStr(subject.createTripSearchMetadata().prevDateTime)
    );
    assertEquals(
      "12:30",
      TestPagingUtils.cleanStr(subject.createTripSearchMetadata().nextDateTime)
    );
  }

  @Test
  @SuppressWarnings("deprecation")
  void testCreateTripSearchMetadataArriveByWithNormalSearchWindow() {
    var model = TestPagingModel.testDataWithFewItinerariesCaseB();
    var testDriver = model.arriveByDriver(T09_15, T12_00, SEARCH_WINDOW_USED, 3);
    var subject = testDriver.pagingService();

    assertEquals(D30m, subject.createTripSearchMetadata().searchWindowUsed);
    assertEquals("8:45", TestPagingUtils.cleanStr(subject.createTripSearchMetadata().prevDateTime));
    assertEquals("9:45", TestPagingUtils.cleanStr(subject.createTripSearchMetadata().nextDateTime));
  }
}
