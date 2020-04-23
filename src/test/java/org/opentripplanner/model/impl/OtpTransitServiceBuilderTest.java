package org.opentripplanner.model.impl;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.FareAttribute;
import org.opentripplanner.model.FareRule;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Frequency;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.ShapePoint;
import org.opentripplanner.model.calendar.ServiceCalendar;
import org.opentripplanner.model.calendar.ServiceCalendarDate;
import org.opentripplanner.model.calendar.ServiceDate;

import java.io.IOException;
import java.util.Collection;

import static java.util.Comparator.comparing;
import static org.junit.Assert.assertEquals;
import static org.opentripplanner.gtfs.GtfsContextBuilder.contextBuilder;

/**
 * @author Thomas Gran (Capra) - tgr@capraconsulting.no (30.10.2017)
 */
public class OtpTransitServiceBuilderTest {
    private static final String FEED_ID = "F";
    private static final FeedScopedId SERVICE_WEEKDAYS_ID = new FeedScopedId(FEED_ID, "weekdays");

    private static OtpTransitServiceBuilder subject;

    @BeforeClass
    public static void setUpClass() throws IOException {
        subject = createBuilder();
    }

    @Test
    public void testGetAllCalendarDates() {
        Collection<ServiceCalendarDate> calendarDates = subject.getCalendarDates();

        assertEquals(1, calendarDates.size());
        assertEquals(
                "<CalendarDate serviceId=F:weekdays date=2017-08-31 exception=2>",
                first(calendarDates).toString());
    }

    @Test
    public void testGetAllCalendars() {
        Collection<ServiceCalendar> calendars = subject.getCalendars();

        assertEquals(2, calendars.size());
        assertEquals("<ServiceCalendar F:alldays [1111111]>", first(calendars).toString());
    }

    @Test
    public void testGetAllFrequencies() {
        Collection<Frequency> frequencies = subject.getFrequencies();

        assertEquals(2, frequencies.size());
        assertEquals(
                "<Frequency trip=agency:15.1 start=06:00:00 end=10:00:01>",
                first(frequencies).toString()
        );
    }

    @Test
    public void testGetRoutes() {
        Collection<Route> routes = subject.getRoutes().values();

        assertEquals(18, routes.size());
        assertEquals("<Route agency:1 1>", first(routes).toString());
    }

    @Test
    public void testGetAllShapePoints() {
        Collection<ShapePoint> shapePoints = subject.getShapePoints().values();

        assertEquals(9, shapePoints.size());
        assertEquals("<ShapePoint F:4 #1 (41.0,-75.0)>", first(shapePoints).toString());
    }


    /* private methods */

    private static OtpTransitServiceBuilder createBuilder() throws IOException {
        OtpTransitServiceBuilder builder = contextBuilder(FEED_ID, ConstantsForTests.FAKE_GTFS).getTransitBuilder();
        Agency agency = agency(builder);

        // Supplement test data with at least one entity in all collections
        builder.getCalendarDates().add(createAServiceCalendarDateExclution(SERVICE_WEEKDAYS_ID));
        builder.getFareAttributes().add(createFareAttribute(agency));
        builder.getFareRules().add(new FareRule());
        builder.getFeedInfos().add(new FeedInfo());

        return builder;
    }

    private static Agency agency(OtpTransitServiceBuilder builder) {
        return first(builder.getAgenciesById().values());
    }

    private static FareAttribute createFareAttribute(Agency agency) {
        FareAttribute fa = new FareAttribute();
        fa.setId(new FeedScopedId(FEED_ID, "FA"));
        return fa;
    }

    private static ServiceCalendarDate createAServiceCalendarDateExclution(FeedScopedId serviceId) {
        return new ServiceCalendarDate(
                serviceId,
                new ServiceDate(2017, 8, 31),
                2
        );
    }

    private static <T> T first(Collection<? extends T> c) {
        return c.stream().min(comparing(T::toString)).orElse(null);
    }
}