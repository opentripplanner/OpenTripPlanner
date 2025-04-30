package org.opentripplanner.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

  private static final int EXACT_TIMES = 1;

  private static final int HEADWAY_SECS = 2;

  private static final int LABEL_ONLY = 1;

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
    FREQUENCY.setExactTimes(EXACT_TIMES);
    FREQUENCY.setHeadwaySecs(HEADWAY_SECS);
    FREQUENCY.setLabelOnly(LABEL_ONLY);
    FREQUENCY.setTrip(DATA.trip);
  }

  @Test
  public void testMapCollection() throws Exception {
    assertNull(subject.map((Collection<Frequency>) null));
    assertTrue(subject.map(Collections.emptyList()).isEmpty());
    assertEquals(1, subject.map(Collections.singleton(FREQUENCY)).size());
  }

  @Test
  public void testMap() throws Exception {
    org.opentripplanner.model.Frequency result = subject.map(FREQUENCY);

    assertEquals(START_TIME, result.getStartTime());
    assertEquals(END_TIME, result.getEndTime());
    assertEquals(EXACT_TIMES, result.getExactTimes());
    assertEquals(HEADWAY_SECS, result.getHeadwaySecs());
    assertEquals(LABEL_ONLY, result.getLabelOnly());
    assertEquals(DATA.trip.getId().getId(), result.getTrip().getId().getId());
  }

  @Test
  public void testMapWithNulls() throws Exception {
    org.opentripplanner.model.Frequency result = subject.map(new Frequency());

    assertEquals(0, result.getStartTime());
    assertEquals(0, result.getEndTime());
    assertEquals(0, result.getExactTimes());
    assertEquals(0, result.getHeadwaySecs());
    assertEquals(0, result.getLabelOnly());
    assertNull(result.getTrip());
  }

  /** Mapping the same object twice, should return the the same instance. */
  @Test
  public void testMapCache() throws Exception {
    org.opentripplanner.model.Frequency result1 = subject.map(FREQUENCY);
    org.opentripplanner.model.Frequency result2 = subject.map(FREQUENCY);

    assertSame(result1, result2);
  }
}
