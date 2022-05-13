package org.opentripplanner.gtfs.mapping;

import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.onebusaway.gtfs.model.Agency;
import org.opentripplanner.transit.model._data.TransitModelForTest;

public class AgencyMapperTest {

  private static final Agency AGENCY = new Agency();

  private static final String ID = "ID";

  private static final String NAME = "Ann";

  private static final String LANG = "NO";

  private static final String PHONE = "+47 987 65 432";

  private static final String TIMEZONE = "GMT";

  private static final String URL = "www.url.com";

  private static final String FARE_URL = "www.url.com/fare";

  private static final String BRANDING_URL = "www.url.com/brand";
  private final AgencyMapper subject = new AgencyMapper(TransitModelForTest.FEED_ID);

  static {
    AGENCY.setId(ID);
    AGENCY.setName(NAME);
    AGENCY.setLang(LANG);
    AGENCY.setPhone(PHONE);
    AGENCY.setTimezone(TIMEZONE);
    AGENCY.setUrl(URL);
    AGENCY.setFareUrl(FARE_URL);
    AGENCY.setBrandingUrl(BRANDING_URL);
  }

  @Test
  public void testMapCollection() throws Exception {
    assertNull(subject.map((Collection<Agency>) null));
    assertTrue(subject.map(Collections.emptyList()).isEmpty());
    assertEquals(1, subject.map(singleton(AGENCY)).size());
  }

  @Test
  public void testMap() throws Exception {
    org.opentripplanner.transit.model.organization.Agency result;

    result = subject.map(AGENCY);

    assertEquals(TransitModelForTest.FEED_ID, result.getId().getFeedId());
    assertEquals(ID, result.getId().getId());
    assertEquals(NAME, result.getName());
    assertEquals(LANG, result.getLang());
    assertEquals(PHONE, result.getPhone());
    assertEquals(TIMEZONE, result.getTimezone());
    assertEquals(URL, result.getUrl());
    assertEquals(FARE_URL, result.getFareUrl());
    assertEquals(BRANDING_URL, result.getBrandingUrl());
  }

  @Test
  public void testMapWithNulls() throws Exception {
    org.opentripplanner.transit.model.organization.Agency result;
    Agency orginal = new Agency();
    orginal.setId(ID);
    orginal.setName(NAME);
    result = subject.map(orginal);

    assertNotNull(result.getId());
    assertNotNull(result.getName());
    assertNull(result.getLang());
    assertNull(result.getPhone());
    assertNull(result.getTimezone());
    assertNull(result.getUrl());
    assertNull(result.getFareUrl());
    assertNull(result.getBrandingUrl());
  }

  /** Mapping the same object twice, should return the the same instance. */
  @Test
  public void testMapCache() throws Exception {
    org.opentripplanner.transit.model.organization.Agency result1 = subject.map(AGENCY);
    org.opentripplanner.transit.model.organization.Agency result2 = subject.map(AGENCY);

    assertSame(result1, result2);
  }
}
