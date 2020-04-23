package org.opentripplanner.netex;

import com.google.common.collect.Multimap;
import org.junit.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.MultiModalStation;
import org.opentripplanner.model.Notice;
import org.opentripplanner.model.Operator;
import org.opentripplanner.model.OtpTransitService;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopTimeKey;
import org.opentripplanner.model.TransitEntity;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.netex.loader.NetexBundle;
import org.opentripplanner.routing.trippattern.Deduplicator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Load a small NeTEx file set without failing. This is just a smoke test
 * and should be excluded from line coverage. The focus of this test is
 * to test that the different parts of the NeTEx works together.
 */
public class NetexLoaderSmokeTest {
    /**
     * This test load a very simple Netex data set and do assertions on it.
     * For each type we assert some of the most important fields for one element
     * and then check the expected number of that type. This is not a replacement
     * for unit tests on mappers. Try to focus on relation between entities and Netex
     * import integration.
     */
    @Test
    public void smokeTestOfNetexLoadData() {
        // Given
        NetexBundle netexBundle = ConstantsForTests.createMinimalNetexBundle();

        // Run the check to make sure it does not throw an exception
        netexBundle.checkInputs();

        // When
        OtpTransitServiceBuilder transitBuilder =
                netexBundle.loadBundle(new Deduplicator(), new DataImportIssueStore(false));

        // Then - smoke test model
        OtpTransitService otpModel = transitBuilder.build();

        assertAgencies(otpModel.getAllAgencies());
        assertMultiModalStations(otpModel.getAllMultiModalStations());
        assertOperators(otpModel.getAllOperators());
        assertStops(otpModel.getAllStops());
        assertStations(otpModel.getAllStations());
        assertTripPatterns(otpModel.getTripPatterns());
        assertTrips(otpModel.getAllTrips());
        assertServiceIds(otpModel.getAllServiceIds());
        assertNoticeAssignments(otpModel.getNoticeAssignments());

        // And then - smoke test service calendar
        assetServiceCalendar(transitBuilder.buildCalendarServiceData());
    }


    /* private methods */

    private void assertAgencies(Collection<Agency> agencies) {
        assertEquals(1, agencies.size());
        Agency a = list(agencies).get(0);
        assertEquals("RUT:Authority:RUT", a.getId().getId());
        assertEquals("RUT", a.getName());
        assertNull( a.getUrl());
        assertEquals("Europe/Oslo", a.getTimezone());
        assertNull(a.getLang());
        assertNull(a.getPhone());
        assertNull( a.getFareUrl());
        assertNull( a.getBrandingUrl());
    }

    private void assertMultiModalStations(Collection<MultiModalStation> multiModalStations) {
        Map<FeedScopedId, MultiModalStation> map = multiModalStations.stream()
                        .collect(Collectors.toMap(MultiModalStation::getId, s -> s));
        MultiModalStation multiModalStation = map.get(fId("NSR:StopPlace:58243"));
        assertEquals("Bergkrystallen", multiModalStation.getName());
        assertEquals(59.866603, multiModalStation.getLat(), 0.000001);
        assertEquals(10.821614, multiModalStation.getLon(), 0.000001);
        assertEquals(3, multiModalStations.size());
    }

    private void assertOperators(Collection<Operator> operators) {
        assertEquals(1, operators.size());
        Operator o = list(operators).get(0);
        assertEquals("RUT:Operator:130c", o.getId().getId());
        assertEquals("Ruter", o.getName());
        assertNull( o.getUrl());
        assertNull(o.getPhone());
    }

    private void assertStops(Collection<Stop> stops) {
        Map<FeedScopedId, Stop> map = stops.stream().collect(Collectors.toMap(Stop::getId, s -> s));

        Stop quay = map.get(fId("NSR:Quay:122003"));
        assertEquals("N/A", quay.getName());
        assertEquals(59.909803, quay.getLat(), 0.000001);
        assertEquals(10.748062, quay.getLon(), 0.000001);
        assertEquals("RB:NSR:StopPlace:3995", quay.getParentStation().getId().toString());
        assertEquals("L", quay.getCode());
        assertEquals(16, stops.size());
    }

    private void assertStations(Collection<Station> stations) {
        Map<FeedScopedId, Station> map = stations.stream().collect(Collectors.toMap(Station::getId, s -> s));
        Station station = map.get(fId("NSR:StopPlace:5825"));
        assertEquals("Bergkrystallen T", station.getName());
        assertEquals(59.866297, station.getLat(), 0.000001);
        assertEquals(10.821484, station.getLon(), 0.000001);
        assertEquals(5, stations.size());
    }

    private void assertTripPatterns(Collection<TripPattern> patterns) {
        Map<FeedScopedId, TripPattern> map = patterns.stream().collect(Collectors.toMap(TripPattern::getId, s -> s));
        TripPattern p = map.get(fId("RUT:JourneyPattern:12-1"));
        assertEquals("Jernbanetorget", p.getDirection());
        assertEquals("RB", p.getFeedId());
        assertEquals("[<Stop RB:NSR:Quay:7203>, <Stop RB:NSR:Quay:8027>]", p.getStops().toString());
        assertEquals("[<Trip RB:RUT:ServiceJourney:12-101375-1000>]", p.getTrips().toString());

        // TODO OTP2 - Why?
        assertNull(p.getServices());
        assertEquals(4, patterns.size());
    }

    private void assertTrips(Collection<Trip> trips) {
        Map<FeedScopedId, Trip> map = trips.stream().collect(Collectors.toMap(Trip::getId, t -> t));
        Trip t = map.get(fId("RUT:ServiceJourney:12-101375-1001"));

        assertEquals("Jernbanetorget", t.getTripHeadsign());
        assertNull(t.getTripShortName());
        assertEquals("RUT:DayType:6-101468", t.getServiceId().getId());
        assertEquals("Ruter", t.getOperator().getName());
        assertNull(t.getTripOperator());
        assertEquals(0, t.getBikesAllowed());
        assertEquals(0, t.getWheelchairAccessible());
        assertEquals(4, trips.size());
    }

    private void assertNoticeAssignments(Multimap<TransitEntity<?>, Notice> map) {
        assertNote(map, fId("RUT:ServiceJourney:4-101468-583"),"045", "Notice on ServiceJourney");
        assertNote(map, stId("RUT:ServiceJourney:4-101468-583", 0), "035", "Notice on TimetabledPassingTime");
        assertNote(map, fId("RUT:Line:4"), "075", "Notice on Line");
        assertNote(map, stId("RUT:ServiceJourney:4-101493-1098", 1), "090", "Notice on Journeypattern");
        assertEquals(4, map.size());
    }

    private void assertNote(Multimap<TransitEntity<?>, Notice> map, Serializable entityKey, String code, String text) {
        TransitEntity<?> key = map.keySet().stream()
                .filter(it -> entityKey.equals(it.getId()))
                .findFirst()
                .orElseThrow(IllegalStateException::new);

        List<Notice> list = list(map.get(key));
        if(list.size() == 0) fail("Notice not found: " + key + " -> <Notice " + code + ", " + text + ">\n\t" + map);
        Notice n = list.get(0);
        assertTrue(n.getId().toString().startsWith("RB:RUT:Notice:"));
        assertEquals(code, n.getPublicCode());
        assertEquals(text, n.getText());
        assertEquals(1, list.size());
    }

    private void assertServiceIds(Collection<FeedScopedId> serviceIds) {
        List<String> sIds = serviceIds.stream().map(FeedScopedId::getId).sorted().collect(Collectors.toList());
        assertEquals(
                "[RUT:DayType:0-105025+RUT:DayType:0-105026+RUT:DayType:6-101468, RUT:DayType:6-101468]",
                sIds.toString()
        );
    }

    private void assetServiceCalendar(CalendarServiceData cal) {
        assertEquals("[RB:RUT:Authority:RUT]", cal.getAgencyIds().toString());
        assertEquals("Europe/Oslo", cal.getTimeZoneForAgencyId(new FeedScopedId("RB", "RUT:Authority:RUT")).toZoneId().toString());
        assertEquals(
                "[RUT:DayType:0-105025+RUT:DayType:0-105026+RUT:DayType:6-101468, RUT:DayType:6-101468]",
                cal.getServiceIds().stream().map(FeedScopedId::getId).sorted().collect(Collectors.toList()).toString()
        );
        assertEquals(
                "[2017-12-21, 2017-12-22, 2017-12-25, 2017-12-26, 2017-12-27, 2017-12-28, 2017-12-29, 2018-01-02, 2018-01-03, 2018-01-04]",
                cal.getServiceDatesForServiceId(fId("RUT:DayType:6-101468")).toString()
        );
        ServiceDate DEC_29 = new ServiceDate(2017, 12, 29);
        assertEquals(
                "RUT:DayType:0-105025+RUT:DayType:0-105026+RUT:DayType:6-101468, RUT:DayType:6-101468",
                cal.getServiceIdsForDate(DEC_29).stream().map(FeedScopedId::getId).sorted().collect(Collectors.joining(", "))
        );
        assertEquals(2, cal.getServiceIds().size());
        assertEquals(1, cal.getAgencyIds().size());
    }

    private static <T> List<T> list(Collection<T> collection) { return new ArrayList<>(collection);}

    private static StopTimeKey stId(String id, int stopSequenceNr) {
        return new StopTimeKey(fId(id), stopSequenceNr);
    }

    private static FeedScopedId fId(String id) {
        return new FeedScopedId("RB", id);
    }
}