package org.opentripplanner.api.ws;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.opentripplanner.util.DateConstants.ONE_DAY_MILLI;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

import org.codehaus.jettison.json.JSONException;
import org.junit.Test;
import org.mockito.Matchers;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.gtfs.services.calendar.CalendarService;
import org.opentripplanner.api.model.Place;
import org.opentripplanner.api.model.transit.StopTimeList;
import org.opentripplanner.api.model.transit.TripTimesPair;
import org.opentripplanner.routing.edgetype.TableTripPattern;
import org.opentripplanner.routing.edgetype.Timetable;
import org.opentripplanner.routing.edgetype.TimetableResolver;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.TransitIndexService;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.updater.stoptime.TimetableSnapshotSource;

public class TransitIndexTest {
    @Test
    public final void testTripTimes() {
        AgencyAndId id = new AgencyAndId("Agency", "ID");
        TransitIndex transitIndex = new TransitIndex();
        Trip trip = new Trip();
        Stop depart = new Stop();
        Stop middle = new Stop();
        Stop arrive = new Stop();
        Set<AgencyAndId> set = new HashSet<AgencyAndId>(1, 1);

        GraphService graphService = mock(GraphService.class);
        Graph graph = mock(Graph.class);
        TransitIndexService transitIndexService = mock(TransitIndexService.class);
        CalendarService calendarService = mock(CalendarService.class);
        TableTripPattern tableTripPattern = mock(TableTripPattern.class);
        TripTimes scheduledTripTimes = mock(TripTimes.class);
        TripTimes resolvedTripTimes = mock(TripTimes.class);
        TimetableSnapshotSource timetableSnapshotSource = mock(TimetableSnapshotSource.class);
        TimetableResolver timetableResolver = mock(TimetableResolver.class);
        Timetable timetable = mock(Timetable.class);

        when(graphService.getGraph(Matchers.anyString())).thenReturn(graph);
        when(graph.getTimeZone()).thenReturn(TimeZone.getTimeZone("UTC"));
        when(graph.getService(TransitIndexService.class)).thenReturn(transitIndexService);
        when(graph.getCalendarService()).thenReturn(calendarService);
        when(graph.getTimetableSnapshotSource()).thenReturn(timetableSnapshotSource);
        when(transitIndexService.getTripPatternForTrip(id)).thenReturn(tableTripPattern);
        when(tableTripPattern.getTrip(Matchers.anyInt())).thenReturn(trip);
        when(tableTripPattern.getTripTimes(Matchers.anyInt())).thenReturn(scheduledTripTimes);
        when(tableTripPattern.getStop(0)).thenReturn(depart);
        when(tableTripPattern.getStop(1)).thenReturn(middle);
        when(tableTripPattern.getStop(2)).thenReturn(arrive);
        when(scheduledTripTimes.getNumHops()).thenReturn(2);
        when(resolvedTripTimes.getNumHops()).thenReturn(2);
        when(resolvedTripTimes.getScheduledDepartureTime(0)).thenReturn(0);
        when(resolvedTripTimes.getScheduledArrivalTime(0)).thenReturn(1);
        when(resolvedTripTimes.getScheduledDepartureTime(1)).thenReturn(2);
        when(resolvedTripTimes.getScheduledArrivalTime(1)).thenReturn(3);
        when(resolvedTripTimes.getDepartureTime(0)).thenReturn(0);
        when(resolvedTripTimes.getArrivalTime(0)).thenReturn(2);
        when(resolvedTripTimes.getDepartureTime(1)).thenReturn(4);
        when(resolvedTripTimes.getArrivalTime(1)).thenReturn(6);
        when(calendarService.getServiceIdsOnDate(Matchers.any(ServiceDate.class))).thenReturn(set);
        when(timetableSnapshotSource.getTimetableSnapshot()).thenReturn(timetableResolver);
        when(timetableResolver.resolve(Matchers.any(TableTripPattern.class),
                Matchers.any(ServiceDate.class))).thenReturn(timetable);
        when(timetable.getTripTimes(Matchers.anyInt())).thenReturn(resolvedTripTimes);

        transitIndex.setGraphService(graphService);
        depart.setName("Name 0");
        depart.setId(new AgencyAndId("Agency", "ID 0"));
        depart.setCode("Code 0");
        depart.setPlatformCode("Platform 0");
        depart.setLon(0.0);
        depart.setLat(0.0);
        depart.setZoneId("Zone 0");
        middle.setName("Name 1");
        middle.setId(new AgencyAndId("Agency", "ID 1"));
        middle.setCode("Code 1");
        middle.setPlatformCode("Platform 1");
        middle.setLon(90.0);
        middle.setLat(45.0);
        middle.setZoneId("Zone 1");
        arrive.setName("Name 2");
        arrive.setId(new AgencyAndId("Agency", "ID 2"));
        arrive.setCode("Code 2");
        arrive.setPlatformCode("Platform 2");
        arrive.setLon(180.0);
        arrive.setLat(90.0);
        arrive.setZoneId("Zone 2");
        set.add(null);

        Object object = transitIndex.getTripTimes(id.getAgencyId(), id.getId(), null, null);
        assertEquals(TripTimesPair.class, object.getClass());

        for (int i = 0; i < 3; i++) {
            Place scheduled = ((TripTimesPair) object).scheduled[i];
            assertEquals("Name " + i, scheduled.name);
            assertEquals("Agency_ID " + i, scheduled.stopId.toString());
            assertEquals("Code " + i, scheduled.stopCode);
            assertEquals("Platform " + i, scheduled.platformCode);
            assertEquals(i * 90.0, scheduled.lon.doubleValue(), 0.0);
            assertEquals(i * 45.0, scheduled.lat, 0.0);
            if (i > 0) {
                assertEquals(i * 2000L - 1000, scheduled.arrival.getTimeInMillis() % ONE_DAY_MILLI);
            }
            if (i < 2) {
                assertEquals(i * 2000L, scheduled.departure.getTimeInMillis() % ONE_DAY_MILLI);
            }
            assertNull(scheduled.orig);
            assertEquals("Zone " + i, scheduled.zoneId);
            assertEquals(i, scheduled.stopIndex.intValue());
            assertEquals(0, scheduled.stopSequence.intValue());

            Place resolved = ((TripTimesPair) object).resolved[i];
            assertEquals("Name " + i, resolved.name);
            assertEquals("Agency_ID " + i, resolved.stopId.toString());
            assertEquals("Code " + i, resolved.stopCode);
            assertEquals("Platform " + i, resolved.platformCode);
            assertEquals(i * 90.0, resolved.lon.doubleValue(), 0.0);
            assertEquals(i * 45.0, resolved.lat, 0.0);
            if (i > 0) {
                assertEquals(i * 4000L - 2000, resolved.arrival.getTimeInMillis() % ONE_DAY_MILLI);
            }
            if (i < 2) {
                assertEquals(i * 4000L, resolved.departure.getTimeInMillis() % ONE_DAY_MILLI);
            }
            assertNull(resolved.orig);
            assertEquals("Zone " + i, resolved.zoneId);
            assertEquals(i, resolved.stopIndex.intValue());
            assertEquals(0, resolved.stopSequence.intValue());
        }
    }

    @Test
    public final void testStopTimesForStation() throws JSONException {
        AgencyAndId station = new AgencyAndId("Agency", "Station");
        Stop stop0 = new Stop();
        Stop stop1 = new Stop();
        ArrayList<Stop> stops = new ArrayList<Stop>(2);
        TransitIndex transitIndex = new TransitIndex();

        GraphService graphService = mock(GraphService.class);
        Graph graph = mock(Graph.class);
        TransitIndexService transitIndexService = mock(TransitIndexService.class);

        when(graphService.getGraph(Matchers.anyString())).thenReturn(graph);
        when(graph.getService(TransitIndexService.class)).thenReturn(transitIndexService);
        when(transitIndexService.getStopsForStation(station)).thenReturn(stops);

        stop0.setId(new AgencyAndId("Agency", "Stop 0"));
        stop1.setId(new AgencyAndId("Agency", "Stop 1"));
        stops.add(stop0);
        stops.add(stop1);
        transitIndex.setGraphService(graphService);

        Object object = transitIndex.getStopTimesForStation("Agency", "Station",
                0, null, null, null, null, null);

        if (object instanceof StopTimeList[]) {
            StopTimeList[] array = (StopTimeList[]) object;
            assertEquals(2, array.length);
            assertEquals(stop0.getId(), array[0].stop);
            assertEquals(stop1.getId(), array[1].stop);
        } else fail("The getStopTimesForStation() method didn't return a StopTimeList[] instance.");
    }
}
