package org.opentripplanner.gtfs.mapping;

import org.junit.Test;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.model.BikeAccess;
import org.opentripplanner.model.Branding;

import java.util.Collection;
import java.util.Collections;
import org.opentripplanner.model.TransitMode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class RouteMapperTest {

    private static final String FEED_ID = "FEED";

    private static final AgencyAndId AGENCY_AND_ID = new AgencyAndId("A", "1");

    private static final String SHORT_NAME = "Short Name";

    private static final String LONG_NAME = "Long Name";

    private static final String DESC = "Desc";

    private static final Integer ROUTE_TYPE = 2;

    private static final TransitMode TRANSIT_MODE = TransitMode.RAIL;

    private static final String URL = "www.url.me";

    private static final String COLOR = "green";

    private static final String TEXT_COLOR = "red";

    private static final int BIKES_ALLOWED = 1;

    private static final int SORT_ORDER = 1;

    private static final String BRANDING_URL = "www.url.me/brand";

    private static final Agency AGENCY = new Agency();

    private static final Route ROUTE = new Route();

    static {
        AGENCY.setId("A");

        ROUTE.setId(AGENCY_AND_ID);
        ROUTE.setAgency(AGENCY);
        ROUTE.setShortName(SHORT_NAME);
        ROUTE.setLongName(LONG_NAME);
        ROUTE.setDesc(DESC);
        ROUTE.setType(ROUTE_TYPE);
        ROUTE.setUrl(URL);
        ROUTE.setColor(COLOR);
        ROUTE.setTextColor(TEXT_COLOR);
        ROUTE.setBikesAllowed(BIKES_ALLOWED);
        ROUTE.setSortOrder(SORT_ORDER);
        ROUTE.setBrandingUrl(BRANDING_URL);
    }

    private final RouteMapper subject =
            new RouteMapper(new AgencyMapper(FEED_ID), new DataImportIssueStore(false));

    @Test
    public void testMapCollection() throws Exception {
        assertNull(null, subject.map((Collection<Route>) null));
        assertTrue(subject.map(Collections.emptyList()).isEmpty());
        assertEquals(1, subject.map(Collections.singleton(ROUTE)).size());
    }

    @Test
    public void testMap() throws Exception {
        org.opentripplanner.model.Route result = subject.map(ROUTE);

        assertEquals("A:1", result.getId().toString());
        assertNotNull(result.getAgency());
        assertEquals(SHORT_NAME, result.getShortName());
        assertEquals(LONG_NAME, result.getLongName());
        assertEquals(DESC, result.getDesc());
        assertEquals(ROUTE_TYPE, result.getGtfsType());
        assertEquals(TRANSIT_MODE, result.getMode());
        assertEquals(URL, result.getUrl());
        assertEquals(COLOR, result.getColor());
        assertEquals(TEXT_COLOR, result.getTextColor());
        assertEquals(BikeAccess.ALLOWED, result.getBikesAllowed());
        assertEquals(SORT_ORDER, result.getSortOrder());

        Branding branding = result.getBranding();
        assertNotNull(branding);
        assertEquals(BRANDING_URL, branding.getUrl());
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
        assertEquals(0, (int) result.getGtfsType());
        assertEquals(TransitMode.TRAM, result.getMode());
        assertNull(result.getUrl());
        assertNull(result.getColor());
        assertNull(result.getTextColor());
        assertEquals(BikeAccess.UNKNOWN, result.getBikesAllowed());
        assertFalse(result.isSortOrderSet());

        Branding branding = result.getBranding();
        assertNull(branding);
    }

    /**
     * Mapping the same object twice, should return the the same instance.
     */
    @Test
    public void testMapCache() throws Exception {
        org.opentripplanner.model.Route result1 = subject.map(ROUTE);
        org.opentripplanner.model.Route result2 = subject.map(ROUTE);

        assertSame(result1, result2);
    }

}