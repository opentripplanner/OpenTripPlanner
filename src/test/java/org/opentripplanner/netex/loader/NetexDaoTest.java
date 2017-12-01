package org.opentripplanner.netex.loader;

import org.junit.Before;
import org.junit.Test;
import org.rutebanken.netex.model.DayTypeAssignment;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.OperatingPeriod;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.Route;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.StopPlace;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


/**
 * @author Thomas Gran (Capra) - tgr@capraconsulting.no (01.12.2017)
 */
public class NetexDaoTest {

    private static final String ID = "ID:A";
    private static final String ID_2 = "ID:B";
    private static final String REF = "REF";
    private static final String REF_2 = "REF-2";
    private static final String TIMEZONE_NO = "Oslo/Norway";
    private static final String TIMEZONE_PST = "PST";

    private NetexDao root;
    private NetexDao child;

    @Before
    public void setup() {
        root = new NetexDao();
        child = new NetexDao(root);
    }

    @Test
    public void lookupStopsById() throws Exception {
        StopPlace stopPlaceA = stopPlace(ID, null);
        StopPlace stopPlaceB = stopPlace(ID, "image_1");

        assertEquals(emptyList(), root.lookupStopPlacesById(ID));
        assertEquals(emptyList(), child.lookupStopPlacesById(ID));

        root.addStopPlace(stopPlaceA);

        assertEquals(singletonList(stopPlaceA), root.lookupStopPlacesById(ID));
        assertEquals(singletonList(stopPlaceA), child.lookupStopPlacesById(ID));

        child.addStopPlace(stopPlaceB);

        assertEquals(singletonList(stopPlaceB), child.lookupStopPlacesById(ID));
    }

    @Test
    public void lookupQuayIdByStopPointRef() throws Exception {
        assertNull(root.lookupQuayIdByStopPointRef(ID));
        assertNull(child.lookupQuayIdByStopPointRef(ID));

        root.addQuayIdByStopPointRef(ID, REF);

        assertEquals(REF, root.lookupQuayIdByStopPointRef(ID));
        assertEquals(REF, child.lookupQuayIdByStopPointRef(ID));

        child.addQuayIdByStopPointRef(ID, REF_2);

        assertEquals(REF_2, child.lookupQuayIdByStopPointRef(ID));
    }

    @Test
    public void lookupQuayById() throws Exception {
        Quay quayA = quay(ID, null);
        Quay quayB = quay(ID, "image_1");

        assertNull(root.lookupQuayById(ID));
        assertNull(child.lookupQuayById(ID));

        root.addQuay(quayA);

        assertEquals(quayA, root.lookupQuayById(ID));
        assertEquals(quayA, child.lookupQuayById(ID));

        child.addQuay(quayB);

        assertEquals(quayB, child.lookupQuayById(ID));
    }

    @Test
    public void lookupDayTypeAvailable() throws Exception {
        assertNull(root.lookupDayTypeAvailable(ID));
        assertNull(child.lookupDayTypeAvailable(ID));

        root.addDayTypeAvailable(ID, TRUE);

        assertEquals(TRUE, root.lookupDayTypeAvailable(ID));
        assertEquals(TRUE, child.lookupDayTypeAvailable(ID));

        child.addDayTypeAvailable(ID, FALSE);

        assertEquals(FALSE, child.lookupDayTypeAvailable(ID));
    }

    @Test
    public void lookupDayTypeAssignment() throws Exception {
        DayTypeAssignment dta = dayTypeAssignment(ID, REF);
        DayTypeAssignment dta2 = dayTypeAssignment(ID, REF_2);

        assertEquals(emptyList(), root.lookupDayTypeAssignment(ID));
        assertEquals(emptyList(), child.lookupDayTypeAssignment(ID));

        root.addDayTypeAssignment(ID, dta);

        assertEquals(singletonList(dta), root.lookupDayTypeAssignment(ID));
        assertEquals(singletonList(dta), child.lookupDayTypeAssignment(ID));

        child.addDayTypeAssignment(ID, dta2);

        assertEquals(singletonList(dta2), child.lookupDayTypeAssignment(ID));
    }

    @Test
    public void getTimeZone() throws Exception {
        assertNull(root.getTimeZone());
        assertNull(child.getTimeZone());

        root.setTimeZone(TIMEZONE_NO);

        assertEquals(TIMEZONE_NO, root.getTimeZone());
        assertEquals(TIMEZONE_NO, child.getTimeZone());

        child.setTimeZone(TIMEZONE_PST);

        assertEquals(TIMEZONE_PST, child.getTimeZone());
    }

    @Test
    public void lookupServiceJourneysById() throws Exception {
        ServiceJourney value = new ServiceJourney();
        root.addServiceJourneyById(ID, value);
        assertEquals(singletonList(value), child.lookupServiceJourneysById(ID));
    }

    @Test
    public void lookupJourneyPatternById() throws Exception {
        JourneyPattern value = new JourneyPattern();
        value.withId(ID);
        root.addJourneyPattern(value);
        assertEquals(value, child.lookupJourneyPatternById(ID));
    }

    @Test
    public void lookupRouteById() throws Exception {
        Route value = new Route();
        value.withId(ID);
        root.addRoute(value);
        assertEquals(value, child.lookupRouteById(ID));
    }

    @Test
    public void lookupOperatingPeriodById() throws Exception {
        OperatingPeriod value = new OperatingPeriod();
        value.withId(ID);

        assertFalse(child.operatingPeriodExist(ID));

        root.addOperatingPeriod(value);

        assertEquals(value, child.lookupOperatingPeriodById(ID));
        assertTrue(child.operatingPeriodExist(ID));
    }

    @Test
    public void operatingPeriodExist() throws Exception {
        Route value = new Route();
        value.withId(ID);
        root.addRoute(value);
    }


    /* private methods */

    private static StopPlace stopPlace(String id, String image) {
        StopPlace stopPlace = new StopPlace();
        stopPlace.setId(id);
        stopPlace.withImage(image);
        return stopPlace;
    }

    private static Quay quay(String id, String image) {
        Quay quay = new Quay();
        quay.setId(id);
        quay.withImage(image);
        return quay;
    }

    private static DayTypeAssignment dayTypeAssignment(String id, String dataSourceRef) {
        DayTypeAssignment value = new DayTypeAssignment();
        value.setId(id);
        value.withDataSourceRef(dataSourceRef);
        return value;
    }
}
