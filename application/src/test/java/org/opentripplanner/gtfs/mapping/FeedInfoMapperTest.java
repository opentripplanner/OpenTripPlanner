package org.opentripplanner.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.onebusaway.gtfs.model.FeedInfo;
import org.onebusaway.gtfs.model.calendar.ServiceDate;

public class FeedInfoMapperTest {

  private static final org.onebusaway.gtfs.model.FeedInfo FEED_INFO =
    new org.onebusaway.gtfs.model.FeedInfo();

  private static final String ID = "45";

  private static final ServiceDate START_DATE = new ServiceDate(2016, 10, 5);

  private static final ServiceDate END_DATE = new ServiceDate(2017, 12, 7);

  private static final String LANG = "US";

  private static final String PUBLISHER_NAME = "Name";

  private static final String PUBLISHER_URL = "www.url.pub";

  private static final String VERSION = "Version";
  private final FeedInfoMapper subject = new FeedInfoMapper(ID);

  static {
    FEED_INFO.setId(ID);
    FEED_INFO.setStartDate(START_DATE);
    FEED_INFO.setEndDate(END_DATE);
    FEED_INFO.setLang(LANG);
    FEED_INFO.setPublisherName(PUBLISHER_NAME);
    FEED_INFO.setPublisherUrl(PUBLISHER_URL);
    FEED_INFO.setVersion(VERSION);
  }

  @Test
  public void testMapCollection() throws Exception {
    assertNull(subject.map((Collection<FeedInfo>) null));
    assertTrue(subject.map(Collections.emptyList()).isEmpty());
    assertEquals(1, subject.map(Collections.singleton(FEED_INFO)).size());
  }

  @Test
  public void testMap() throws Exception {
    org.opentripplanner.model.FeedInfo result = subject.map(FEED_INFO);

    assertEquals(ID, result.getId());
    assertEquals("2016-10-05", result.getStartDate().toString());
    assertEquals("2017-12-07", result.getEndDate().toString());
    assertEquals(LANG, result.getLang());
    assertEquals(PUBLISHER_NAME, result.getPublisherName());
    assertEquals(PUBLISHER_URL, result.getPublisherUrl());
    assertEquals(VERSION, result.getVersion());
  }

  @Test
  public void testMapWithNulls() throws Exception {
    org.opentripplanner.model.FeedInfo result = subject.map(new FeedInfo());

    assertNotNull(result.getId());
    assertNull(result.getStartDate());
    assertNull(result.getEndDate());
    assertNull(result.getLang());
    assertNull(result.getPublisherName());
    assertNull(result.getPublisherUrl());
    assertNull(result.getVersion());
  }

  /** Mapping the same object twice, should return the the same instance. */
  @Test
  public void testMapCache() throws Exception {
    org.opentripplanner.model.FeedInfo result1 = subject.map(FEED_INFO);
    org.opentripplanner.model.FeedInfo result2 = subject.map(FEED_INFO);

    assertSame(result1, result2);
  }
}
