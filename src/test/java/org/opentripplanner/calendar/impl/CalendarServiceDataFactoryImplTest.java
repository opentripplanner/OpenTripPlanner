package org.opentripplanner.calendar.impl;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.gtfs.GtfsContextBuilder;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.CalendarService;
import org.opentripplanner.model.FareAttribute;
import org.opentripplanner.model.FareRule;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.model.ServiceCalendarDate;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.model.calendar.LocalizedServiceId;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import static java.util.Arrays.asList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opentripplanner.calendar.impl.CalendarServiceDataFactoryImpl.createCalendarService;
import static org.opentripplanner.calendar.impl.CalendarServiceDataFactoryImpl.createCalendarServiceData;
import static org.opentripplanner.calendar.impl.CalendarServiceDataFactoryImpl.merge;
import static org.opentripplanner.gtfs.GtfsContextBuilder.contextBuilder;
import static org.opentripplanner.model.ServiceCalendarDate.EXCEPTION_TYPE_REMOVE;

/**
 * @author Thomas Gran (Capra) - tgr@capraconsulting.no (08.11.2017)
 */
public class CalendarServiceDataFactoryImplTest {

    private static final String AGENCY = "agency";

    private static final String FEED_ID = "F";

    private static final FeedScopedId SERVICE_ALLDAYS_ID = new FeedScopedId(FEED_ID, "alldays");

    private static final FeedScopedId SERVICE_WEEKDAYS_ID = new FeedScopedId(FEED_ID, "weekdays");

    private static final ServiceDate A_FRIDAY = new ServiceDate(2009, 1, 2);

    private static final ServiceDate A_SUNDAY = new ServiceDate(2009, 1, 4);

    private static final ServiceDate A_MONDAY = new ServiceDate(2009, 1, 5);

    private static CalendarServiceData data;

    private static CalendarService calendarService;

    @BeforeClass
    public static void setup() throws IOException {
        OtpTransitServiceBuilder transitBuilder = createCtxBuilder().getTransitBuilder();
        data = createCalendarServiceData(transitBuilder);
        calendarService = createCalendarService(transitBuilder);
    }

    @Test
    public void testMerge() {
        Set<Character> result = merge(asList('A', 'B'), asList('B', 'C'));

        assertTrue(result.toString(), result.containsAll(asList('A', 'B', 'C')));
        assertEquals(3, result.size());
    }

    @Test
    public void testDataGetServiceIds() throws IOException {
        assertEquals("[F_alldays, F_weekdays]", toString(data.getServiceIds()));
    }

    @Test
    public void testDataGetServiceDatesForServiceId() throws IOException {
        List<ServiceDate> alldays = data.getServiceDatesForServiceId(SERVICE_ALLDAYS_ID);
        assertEquals("[20090101, 20090102, 20090103, 20090104, 20090106, 20090107, 20090108]",
                sevenFirstDays(alldays).toString());
        assertEquals(14975, alldays.size());

        List<ServiceDate> weekdays = data.getServiceDatesForServiceId(SERVICE_WEEKDAYS_ID);
        assertEquals("[20090101, 20090102, 20090105, 20090106, 20090107, 20090108, 20090109]",
                sevenFirstDays(weekdays).toString());
        assertEquals(10697, weekdays.size());
    }

    @Test
    public void testDataGetTimeZoneForAgencyId() throws IOException {
        assertEquals("America/New_York", data.getTimeZoneForAgencyId(AGENCY).getID());
    }

    @Test
    public void testDataGetLocalizedServiceIds() throws IOException {
        LocalizedServiceId locServiceId = getWeekdaysLocalizedServiceId();

        assertEquals("ServiceId(id=F_weekdays timeZone=America/New_York)", locServiceId.toString());
        assertEquals(2, data.getLocalizedServiceIds().size());
    }

    @Test
    public void testDataGetDatesForLocalizedServiceId() throws IOException {
        LocalizedServiceId locServiceId = getWeekdaysLocalizedServiceId();

        SimpleDateFormat formatDate = new SimpleDateFormat("yyyy-MM-dd", Locale.FRENCH);

        assertEquals("[2009-01-01, 2009-01-02, 2009-01-05, 2009-01-06, 2009-01-07]",
                data.getDatesForLocalizedServiceId(locServiceId).stream().limit(5)
                        .map(formatDate::format).collect(toList()).toString());
    }

    @Test
    public void testServiceGetServiceIdsOnDate() throws IOException {
        Set<FeedScopedId> servicesOnFriday = calendarService.getServiceIdsOnDate(A_FRIDAY);
        assertEquals("[F_alldays, F_weekdays]", sort(servicesOnFriday).toString());

        Set<FeedScopedId> servicesOnSunday = calendarService.getServiceIdsOnDate(A_SUNDAY);
        assertEquals("[F_alldays]", servicesOnSunday.toString());

        // Test exclusion of serviceCalendarDate
        Set<FeedScopedId> servicesOnMonday = calendarService.getServiceIdsOnDate(A_MONDAY);
        assertEquals("[F_weekdays]", servicesOnMonday.toString());
    }

    @Test
    public void testServiceGetServiceIds() throws IOException {
        Set<FeedScopedId> serviceIds = calendarService.getServiceIds();
        assertEquals("[F_alldays, F_weekdays]", sort(serviceIds).toString());
    }

    @Test
    public void testServiceGetTimeZoneForAgencyId() throws IOException {
        TimeZone result = calendarService.getTimeZoneForAgencyId(AGENCY);
        assertEquals("America/New_York", result.getID());
    }

    @Test
    public void testServiceGetServiceDatesForServiceId() throws IOException {
        Set<ServiceDate> alldays = calendarService.getServiceDatesForServiceId(SERVICE_ALLDAYS_ID);

        assertTrue(alldays.contains(A_FRIDAY));
        assertTrue(alldays.contains(A_SUNDAY));
        assertEquals(14975, alldays.size());

        Set<ServiceDate> weekdays = calendarService
                .getServiceDatesForServiceId(SERVICE_WEEKDAYS_ID);

        assertTrue(weekdays.contains(A_FRIDAY));
        assertFalse(weekdays.contains(A_SUNDAY));
        assertEquals(10697, weekdays.size());
    }

    private LocalizedServiceId getWeekdaysLocalizedServiceId() {
        Set<LocalizedServiceId> localizedServiceIds = data.getLocalizedServiceIds();
        return localizedServiceIds.stream().filter(it -> it.getId().equals(SERVICE_WEEKDAYS_ID))
                .findFirst().orElseThrow(IllegalStateException::new);
    }

    private static GtfsContext createCtxBuilder() throws IOException {
        GtfsContextBuilder ctxBuilder = contextBuilder(FEED_ID, ConstantsForTests.FAKE_GTFS);
        OtpTransitServiceBuilder builder = ctxBuilder.getTransitBuilder();
        Agency agency = agency(builder);

        // Supplement test data with at least one entity in all collections
        builder.getCalendarDates().add(removeMondayFromAlldays());
        builder.getFareAttributes().add(createFareAttribute(agency));
        builder.getFareRules().add(new FareRule());
        builder.getFeedInfos().add(new FeedInfo());

        return ctxBuilder.build();
    }

    private static Agency agency(OtpTransitServiceBuilder builder) {
        return first(builder.getAgencies());
    }

    private static FareAttribute createFareAttribute(Agency agency) {
        FareAttribute fa = new FareAttribute();
        fa.setId(new FeedScopedId(agency.getId(), "FA"));
        return fa;
    }

    private static ServiceCalendarDate removeMondayFromAlldays() {
        ServiceCalendarDate date = new ServiceCalendarDate();
        date.setServiceId(SERVICE_ALLDAYS_ID);
        date.setDate(new ServiceDate(2009, 1, 5));
        date.setExceptionType(EXCEPTION_TYPE_REMOVE);
        return date;
    }

    private static <T> List<T> sort(Collection<? extends T> c) {
        return c.stream().sorted(comparing(T::toString)).collect(toList());
    }

    private static <T> T first(Collection<? extends T> c) {
        return sort(c).get(0);
    }

    private static <T extends Comparable<T>> String toString(Collection<T> c) {
        return c.stream().sorted(comparing(T::toString)).collect(toList()).toString();
    }

    private static List<String> sevenFirstDays(List<ServiceDate> dates) {
        return dates.stream().limit(7).map(ServiceDate::getAsString).collect(toList());
    }
}