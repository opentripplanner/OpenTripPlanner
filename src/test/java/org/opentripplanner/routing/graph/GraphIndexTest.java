package org.opentripplanner.routing.graph;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.opentripplanner.GtfsTest;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStop;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

import java.text.ParseException;
import java.util.List;
import java.util.Map;

/**
 * Check that the graph index is created, that GTFS elements can be found in the index, and that
 * the indexes are coherent with one another.
 *
 * TODO: The old transit index doesn't exist anymore, and the new one needs more tests.
 */
public class GraphIndexTest extends GtfsTest {

    @Override
    public String getFeedName() {
        return "testagency.zip";
    }

    public void testIdLookup() {

        /* Graph vertices */
        for (Vertex vertex : graph.index.vertexForId.values()) {
            if (vertex instanceof TransitStop) {
                Stop stop = ((TransitStop)vertex).getStop();
                Vertex index_vertex = graph.index.stopVertexForStop.get(stop);
                assertEquals(index_vertex, vertex);
            }
        }

        /* Agencies */
        Agency agency;
        agency = graph.index.agencyForId.get("azerty");
        assertNull(agency);
        agency = graph.index.agencyForId.get("agency");
        assertEquals(agency.getId(), "agency");
        assertEquals(agency.getName(), "Fake Agency");

        /* Stops */
        graph.index.stopForId.get(new AgencyAndId("X", "Y"));

        /* Trips */
//        graph.index.tripForId;
//        graph.index.routeForId;
//        graph.index.serviceForId;
//        graph.index.patternForId;
    }

    /** Check that bidirectional relationships between TripPatterns and Trips, Routes, and Stops are coherent. */
    public void testPatternsCoherent() {
        for (Trip trip : graph.index.tripForId.values()) {
            TripPattern pattern = graph.index.patternForTrip.get(trip);
            assertTrue(pattern.getTrips().contains(trip));
        }
        /* This one depends on a feed where each TripPattern appears on only one route. */
        for (Route route : graph.index.routeForId.values()) {
            for (TripPattern pattern : graph.index.patternsForRoute.get(route)) {
                assertEquals(pattern.route, route);
            }
        }
        for (Stop stop : graph.index.stopForId.values()) {
            for (TripPattern pattern : graph.index.patternsForStop.get(stop)) {
                assertTrue(pattern.stopPattern.containsStop(stop.getId().toString()));
            }
        }
    }

    public void testSpatialIndex() {
        Stop stopJ = graph.index.stopForId.get(new AgencyAndId("agency", "J"));
        Stop stopL = graph.index.stopForId.get(new AgencyAndId("agency", "L"));
        Stop stopM = graph.index.stopForId.get(new AgencyAndId("agency", "M"));
        TransitStop stopvJ = graph.index.stopVertexForStop.get(stopJ);
        TransitStop stopvL = graph.index.stopVertexForStop.get(stopL);
        TransitStop stopvM = graph.index.stopVertexForStop.get(stopM);
        // There are a two other stops within 100 meters of stop J.
        Envelope env = new Envelope(new Coordinate(stopJ.getLon(), stopJ.getLat()));
        env.expandBy(SphericalDistanceLibrary.metersToLonDegrees(100, stopJ.getLat()),
                SphericalDistanceLibrary.metersToDegrees(100));
        List<TransitStop> stops = graph.index.stopSpatialIndex.query(env);
        assertTrue(stops.contains(stopvJ));
        assertTrue(stops.contains(stopvL));
        assertTrue(stops.contains(stopvM));
        assertTrue(stops.size() >= 3); // Query can overselect
    }

    public void testParentStations() {
        // graph.index.stopsForParentStation;
    }

    public void testLucene() {
        // graph.index.luceneIndex
    }

    public void testTimetable() {
        ServiceDate date;
        try {
            date = ServiceDate.parseString("20100101");  // Friday
        } catch (ParseException e){
            return;
        }

        List<Map<Integer, Integer>> expected = Lists.newArrayList(
                ImmutableMap.of(0, 18000, 1, 19800, 2, 21600),
                // The second stop on second trip isn't a timepoint
                ImmutableMap.of(0, 82800, 2, 86400),
                ImmutableMap.of(0, 85200, 1, 87000, 2, 88800));
        assertEquals(
            expected,
            graph.index.timetableForPattern(
                graph.index.patternsForRoute.get(graph.index.routeForId.get(new AgencyAndId("agency", "4")))
                    .iterator().next(),
                date));
    }

    public void testTimetableOnDayWithoutService() {
        ServiceDate date;
        try {
            date = ServiceDate.parseString("20100102");  // Saturday
        } catch (ParseException e){
            return;
        }

        assertEquals(
            Lists.newArrayList(), // Route 4 has service only on weekdays, so result should be empty
            graph.index.timetableForPattern(
                graph.index.patternsForRoute.get(graph.index.routeForId.get(new AgencyAndId("agency", "4")))
                    .iterator().next(),
                date));
    }
}
