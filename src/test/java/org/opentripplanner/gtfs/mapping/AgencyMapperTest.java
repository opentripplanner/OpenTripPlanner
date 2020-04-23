package org.opentripplanner.gtfs.mapping;

import org.junit.Test;
import org.onebusaway.gtfs.model.Agency;

import java.util.Collection;
import java.util.Collections;

import static java.util.Collections.singleton;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AgencyMapperTest {
    private static final String FEED_ID = "FEED";

    private static final Agency AGENCY = new Agency();

    private static final String ID = "ID";

    private static final String NAME = "Ann";

    private static final String LANG = "NO";

    private static final String PHONE = "+47 987 65 432";

    private static final String TIMEZONE = "GMT";

    private static final String URL = "www.url.com";

    private static final String FARE_URL = "www.url.com/fare";

    private static final String BRANDING_URL = "www.url.com/brand";

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

    private AgencyMapper subject = new AgencyMapper(FEED_ID);

    @Test
    public void testMapCollection() throws Exception {
        assertNull(subject.map((Collection<Agency>) null));
        assertTrue(subject.map(Collections.emptyList()).isEmpty());
        assertEquals(1, subject.map(singleton(AGENCY)).size());
    }

    @Test
    public void testMap() throws Exception {
        org.opentripplanner.model.Agency result = subject.map(AGENCY);

        assertEquals("FEED:ID", result.getId().toString());
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
        Agency orginal = new Agency();
        orginal.setId(ID);
        org.opentripplanner.model.Agency result = subject.map(orginal);

        assertNotNull(result.getId());
        assertNull(result.getName());
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
        org.opentripplanner.model.Agency result1 = subject.map(AGENCY);
        org.opentripplanner.model.Agency result2 = subject.map(AGENCY);

        assertTrue(result1 == result2);
    }
}