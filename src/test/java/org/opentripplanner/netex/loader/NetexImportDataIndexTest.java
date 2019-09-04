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

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


/**
 * @author Thomas Gran (Capra) - tgr@capraconsulting.no (01.12.2017)
 */
public class NetexImportDataIndexTest {

    private static final String ID = "ID:A";
    private static final String ID_2 = "ID:B";
    private static final String REF = "REF";
    private static final String REF_2 = "REF-2";
    private static final String TIMEZONE_NO = "Oslo/Norway";
    private static final String TIMEZONE_PST = "PST";

    private NetexImportDataIndex root;
    private NetexImportDataIndex child;

    @Before
    public void setup() {
        root = new NetexImportDataIndex();
        child = new NetexImportDataIndex(root);
    }

    @Test
    public void lookupStopsById() {
        StopPlace stopPlaceA = stopPlace(ID, null);
        StopPlace stopPlaceB = stopPlace(ID, "image_1");

        root.stopPlaceById.add(stopPlaceA);
        child.stopPlaceById.add(stopPlaceB);

        assertEquals(singletonList(stopPlaceA), root.stopPlaceById.lookup(ID));
        assertEquals(singletonList(stopPlaceB), child.stopPlaceById.lookup(ID));
        assertTrue(child.stopPlaceById.lookup(ID_2).isEmpty());
    }

    @Test
    public void lookupQuayIdByStopPointRef() {
        root.quayIdByStopPointRef.add(ID, REF);
        child.quayIdByStopPointRef.add(ID, REF_2);

        assertEquals(REF, root.quayIdByStopPointRef.lookup(ID));
        assertEquals(REF_2, child.quayIdByStopPointRef.lookup(ID));
        assertNull(root.quayIdByStopPointRef.lookup(ID_2));
    }

    @Test
    public void lookupQuayById() {
        Quay quayA = quay(ID, null);
        Quay quayB = quay(ID, "image_1");

        root.quayById.add(quayA);
        child.quayById.add(quayB);

        assertEquals(singletonList(quayA), root.quayById.lookup(ID));
        assertEquals(singletonList(quayB), child.quayById.lookup(ID));
        assertTrue(child.quayById.lookup(ID_2).isEmpty());
    }

    @Test
    public void lookupDayTypeAssignment() {
        DayTypeAssignment dtaA = dayTypeAssignment(ID, REF);
        DayTypeAssignment dtaB = dayTypeAssignment(ID, REF_2);

        root.dayTypeAssignmentByDayTypeId.add(ID, dtaA);
        child.dayTypeAssignmentByDayTypeId.add(ID, dtaB);

        assertEquals(singletonList(dtaA), root.dayTypeAssignmentByDayTypeId.lookup(ID));
        assertEquals(singletonList(dtaB), child.dayTypeAssignmentByDayTypeId.lookup(ID));
        assertTrue(child.dayTypeAssignmentByDayTypeId.lookup(ID_2).isEmpty());
    }

    @Test
    public void getTimeZone() {
        assertNull(root.timeZone.get());
        assertNull(child.timeZone.get());

        root.timeZone.set(TIMEZONE_NO);
        child.timeZone.set(TIMEZONE_PST);

        assertEquals(TIMEZONE_NO, root.timeZone.get());
        assertEquals(TIMEZONE_PST, child.timeZone.get());
    }

    @Test
    public void lookupServiceJourneysById() {
        ServiceJourney value = new ServiceJourney().withId(ID_2);
        root.serviceJourneyByPatternId.add(ID, value);
        assertEquals(singletonList(value), child.serviceJourneyByPatternId.lookup(ID));
    }

    @Test
    public void lookupJourneyPatternById() {
        JourneyPattern value = new JourneyPattern().withId(ID);
        root.journeyPatternsById.add(value);
        assertEquals(value, child.journeyPatternsById.lookup(ID));
    }

    @Test
    public void lookupRouteById() {
        Route value = new Route().withId(ID);
        root.routeById.add(value);
        assertEquals(value, child.routeById.lookup(ID));
    }

    @Test
    public void lookupOperatingPeriodById() {
        OperatingPeriod value = new OperatingPeriod().withId(ID);

        root.operatingPeriodById.add(value);

        assertEquals(value, child.operatingPeriodById.lookup(ID));
        assertTrue(child.operatingPeriodById.containsKey(ID));
    }

    @Test
    public void operatingPeriodExist() {
        Route value = new Route().withId(ID);
        root.routeById.add(value);
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
