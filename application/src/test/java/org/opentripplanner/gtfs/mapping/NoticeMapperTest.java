package org.opentripplanner.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.onebusaway.gtfs.model.AgencyAndIdFactory.obaId;
import static org.opentripplanner.transit.model._data.TransitRepositoryForTest.FEED_ID;

import org.junit.jupiter.api.Test;

class NoticeMapperTest {

  private static final String NOTICE_ID = "N1";
  private static final String DISPLAY_TEXT = "Platform change";

  private static final org.onebusaway.gtfs.model.Notice GTFS_NOTICE;

  static {
    GTFS_NOTICE = new org.onebusaway.gtfs.model.Notice();
    GTFS_NOTICE.setId(obaId(NOTICE_ID));
    GTFS_NOTICE.setDisplayText(DISPLAY_TEXT);
  }

  private final NoticeMapper subject = new NoticeMapper(new IdFactory(FEED_ID));

  @Test
  void testMap() {
    var result = subject.map(GTFS_NOTICE);

    assertEquals(FEED_ID, result.getId().getFeedId());
    assertEquals(NOTICE_ID, result.getId().getId());
    assertEquals(DISPLAY_TEXT, result.text());
  }

  @Test
  void testPublicCodeIsNull() {
    var result = subject.map(GTFS_NOTICE);
    assertNull(result.publicCode());
  }

  @Test
  void testMapCacheReturnsSameInstance() {
    var result1 = subject.map(GTFS_NOTICE);
    var result2 = subject.map(GTFS_NOTICE);
    assertSame(result1, result2);
  }
}
