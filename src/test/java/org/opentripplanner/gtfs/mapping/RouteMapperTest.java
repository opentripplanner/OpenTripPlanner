package org.opentripplanner.gtfs.mapping;

import org.junit.Test;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;

import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class RouteMapperTest {

    private static final AgencyAndId AGENCY_AND_ID = new AgencyAndId("A", "1");

    private static final String SHORT_NAME = "Short Name";

    private static final String LONG_NAME = "Long Name";

    private static final String DESC = "Desc";

    private static final int TYPE = 2;

    private static final String URL = "www.url.me";

    private static final String COLOR = "green";

    private static final String TEXT_COLOR = "red";

    private static final int BIKES_ALLOWED = 2;

    private static final int SORT_ORDER = 1;

    private static final String BRANDING_URL = "www.url.me/brand";

    private static final int ROUTE_BIKES_ALLOWED = 2;

    private static final Agency AGENCY = new Agency();

    private static final Route ROUTE = new Route();

    static {
        AGENCY.setId("A");

        ROUTE.setId(AGENCY_AND_ID);
        ROUTE.setAgency(AGENCY);
        ROUTE.setShortName(SHORT_NAME);
        ROUTE.setLongName(LONG_NAME);
        ROUTE.setDesc(DESC);
        ROUTE.setType(TYPE);
        ROUTE.setUrl(URL);
        ROUTE.setColor(COLOR);
        ROUTE.setTextColor(TEXT_COLOR);
        ROUTE.setBikesAllowed(BIKES_ALLOWED);
        ROUTE.setSortOrder(SORT_ORDER);
        ROUTE.setBrandingUrl(BRANDING_URL);
        ROUTE.setRouteBikesAllowed(ROUTE_BIKES_ALLOWED);
    }

    private RouteMapper subject = new RouteMapper(new AgencyMapper());

    @Test
    public void testMapCollection() throws Exception {
        assertNull(null, subject.map((Collection<Route>) null));
        assertTrue(subject.map(Collections.emptyList()).isEmpty());
        assertEquals(1, subject.map(Collections.singleton(ROUTE)).size());
    }

    @Test
    public void testMap() throws Exception {
        org.opentripplanner.model.Route result = subject.map(ROUTE);

        assertEquals("A_1", result.getId().toString());
        assertNotNull(result.getAgency());
        assertEquals(SHORT_NAME, result.getShortName());
        assertEquals(LONG_NAME, result.getLongName());
        assertEquals(DESC, result.getDesc());
        assertEquals(TYPE, result.getType());
        assertEquals(URL, result.getUrl());
        assertEquals(COLOR, result.getColor());
        assertEquals(TEXT_COLOR, result.getTextColor());
        assertEquals(BIKES_ALLOWED, result.getBikesAllowed());
        assertEquals(SORT_ORDER, result.getSortOrder());
        assertEquals(BRANDING_URL, result.getBrandingUrl());
        assertEquals(BIKES_ALLOWED, result.getRouteBikesAllowed());
    }

    @Test
    public void testMapWithNulls() throws Exception {
        Route input = new Route();
        input.setId(AGENCY_AND_ID);

        org.opentripplanner.model.Route result = subject.map(input);

        assertNotNull(result.getId());
        assertNull(result.getAgency());
        assertNull(result.getShortName());
        assertNull(result.getLongName());
        assertNull(result.getDesc());
        assertEquals(0, result.getType());
        assertNull(result.getUrl());
        assertNull(result.getColor());
        assertNull(result.getTextColor());
        assertEquals(0, result.getBikesAllowed());
        assertFalse(result.isSortOrderSet());
        assertNull(result.getBrandingUrl());
        assertEquals(0, result.getRouteBikesAllowed());
    }

    /**
     * Mapping the same object twice, should return the the same instance.
     */
    @Test
    public void testMapCache() throws Exception {
        org.opentripplanner.model.Route result1 = subject.map(ROUTE);
        org.opentripplanner.model.Route result2 = subject.map(ROUTE);

        assertTrue(result1 == result2);
    }

}