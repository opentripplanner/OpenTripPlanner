package org.opentripplanner.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.onebusaway.gtfs.model.Frequency;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;

public class FrequencyMapperTest {

  private static final GtfsTestData DATA = new GtfsTestData();

  private static final String FEED_ID = "FEED";

  private static final Integer ID = 45;

  private static final int START_TIME = 1200;

  private static final int END_TIME = 2300;

  private static final int HEADWAY_SECS = 2;

  public static final DataImportIssueStore ISSUE_STORE = DataImportIssueStore.NOOP;

  private static final TranslationHelper translationHelper = new TranslationHelper();
  private static final IdFactory ID_FACTORY = new IdFactory(FEED_ID);
  private static final TripMapper TRIP_MAPPER = new TripMapper(
    ID_FACTORY,
    new RouteMapper(ID_FACTORY, new AgencyMapper(ID_FACTORY), ISSUE_STORE, translationHelper),
    new DirectionMapper(ISSUE_STORE),
    translationHelper
  );

  private static final Frequency FREQUENCY = new Frequency();

  private final FrequencyMapper subject;

  {
    subject = new FrequencyMapper(TRIP_MAPPER);
  }

  static {
    FREQUENCY.setId(ID);
    FREQUENCY.setStartTime(START_TIME);
    FREQUENCY.setEndTime(END_TIME);
    FREQUENCY.setExactTimes(1);
    FREQUENCY.setHeadwaySecs(HEADWAY_SECS);
    FREQUENCY.setTrip(DATA.trip);
  }

  @Test
  public void testMapCollection() {
    assertNull(subject.map((Collection<Frequency>) null));
    assertTrue(subject.map(Collections.emptyList()).isEmpty());
    assertEquals(1, subject.map(Collections.singleton(FREQUENCY)).size());
  }

  @Test
  public void testMap() {
    org.opentripplanner.model.Frequency result = subject.map(FREQUENCY);

    assertEquals(START_TIME, result.startTime());
    assertEquals(END_TIME, result.endTime());
    assertTrue(result.exactTimes());
    assertEquals(HEADWAY_SECS, result.headwaySecs());
    assertEquals(DATA.trip.getId().getId(), result.trip().getId().getId());
  }

  @Test
  public void testMapWithNulls() {
    org.opentripplanner.model.Frequency result = subject.map(new Frequency());

    assertEquals(0, result.startTime());
    assertEquals(0, result.endTime());
    assertFalse(result.exactTimes());
    assertEquals(0, result.headwaySecs());
    assertNull(result.trip());
  }

  /** Mapping the same object twice, should return the same instance. */
  @Test
  public void testMapCache() throws Exception {
    org.opentripplanner.model.Frequency result1 = subject.map(FREQUENCY);
    org.opentripplanner.model.Frequency result2 = subject.map(FREQUENCY);

    assertSame(result1, result2);
  }
}
