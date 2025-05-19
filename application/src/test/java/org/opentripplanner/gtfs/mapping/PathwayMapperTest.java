package org.opentripplanner.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Pathway;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.transit.model.site.PathwayMode;
import org.opentripplanner.transit.service.SiteRepository;

public class PathwayMapperTest {

  private static final AgencyAndId AGENCY_AND_ID = new AgencyAndId("A", "1");

  private static final int TRAVERSAL_TIME = 3000;

  private static final TranslationHelper TRANSLATION_HELPER = new TranslationHelper();

  private static final Pathway PATHWAY = new Pathway();

  private static final Stop FROM_STOP = new Stop();

  private static final Stop TO_STOP = new Stop();
  private static final IdFactory ID_FACTORY = new IdFactory("A");

  private final PathwayMapper subject = new PathwayMapper(
    ID_FACTORY,
    new StopMapper(
      ID_FACTORY,
      TRANSLATION_HELPER,
      stationId -> null,
      new SiteRepository().withContext()
    ),
    new EntranceMapper(ID_FACTORY, TRANSLATION_HELPER, stationId -> null),
    new PathwayNodeMapper(ID_FACTORY, TRANSLATION_HELPER, stationId -> null),
    new BoardingAreaMapper(ID_FACTORY, TRANSLATION_HELPER, stationId -> null)
  );

  static {
    FROM_STOP.setId(AGENCY_AND_ID);
    TO_STOP.setId(AGENCY_AND_ID);

    PATHWAY.setId(AGENCY_AND_ID);
    PATHWAY.setFromStop(FROM_STOP);
    PATHWAY.setToStop(TO_STOP);
    PATHWAY.setPathwayMode(2);
    PATHWAY.setTraversalTime(TRAVERSAL_TIME);
  }

  @Test
  public void testMapCollection() throws Exception {
    assertNull(subject.map((Collection<Pathway>) null));
    assertTrue(subject.map(Collections.emptyList()).isEmpty());
    assertEquals(1, subject.map(Collections.singleton(PATHWAY)).size());
  }

  @Test
  public void testMap() throws Exception {
    org.opentripplanner.transit.model.site.Pathway result = subject.map(PATHWAY);

    assertEquals("A:1", result.getId().toString());
    assertNotNull(result.getFromStop());
    assertNotNull(result.getToStop());
    assertEquals(PathwayMode.STAIRS, result.getPathwayMode());
    assertEquals(TRAVERSAL_TIME, result.getTraversalTime());
  }

  @Test
  public void testMapWithNulls() throws Exception {
    Pathway input = new Pathway();
    input.setId(AGENCY_AND_ID);
    input.setFromStop(FROM_STOP);
    input.setToStop(TO_STOP);

    org.opentripplanner.transit.model.site.Pathway result = subject.map(input);

    assertNotNull(result.getId());
    assertNotNull(result.getFromStop());
    assertNotNull(result.getToStop());
    assertEquals(PathwayMode.UNKNOWN, result.getPathwayMode());
    assertEquals(0, result.getTraversalTime());
  }

  /** Mapping the same object twice, should return the the same instance. */
  @Test
  public void testMapCache() throws Exception {
    org.opentripplanner.transit.model.site.Pathway result1 = subject.map(PATHWAY);
    org.opentripplanner.transit.model.site.Pathway result2 = subject.map(PATHWAY);

    assertSame(result1, result2);
  }
}
