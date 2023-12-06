package org.opentripplanner.service.paging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.service.paging.TestPagingModel.D30m;

import java.time.Duration;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.time.TimeUtils;

/**
 * This test the entire paging service module.
 * <p>
 * To debug this test, set the
 */
@SuppressWarnings("DataFlowIssue")
class PS1_LegacyMetaDataTest {

  private static final int T12_00 = TimeUtils.time("12:00");
  private static final int T12_30 = TimeUtils.time("12:30");
  private static final int T13_00 = TimeUtils.time("13:00");
  private static final int T13_30 = TimeUtils.time("13:30");

  private static final Duration SEARCH_WINDOW_USED = D30m;

  private final TestPagingModel model = TestPagingModel.testDataWithManyItinerariesCaseA();

  @Test
  @SuppressWarnings("deprecation")
  void testCreateTripSearchMetadataDepartAfter() {
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
  @Disabled("FIX: This test should not fail!")
  void testCreateTripSearchMetadataArriveBy() {
    var testDriver = model.arriveByDriver(T12_00, T13_00, SEARCH_WINDOW_USED, 3);
    var subject = testDriver.pagingService();

    assertEquals(D30m, subject.createTripSearchMetadata().searchWindowUsed);
    assertEquals(
      "11:40",
      TestPagingUtils.cleanStr(subject.createTripSearchMetadata().prevDateTime)
    );
    assertEquals(
      "12:30",
      TestPagingUtils.cleanStr(subject.createTripSearchMetadata().nextDateTime)
    );
  }
}
