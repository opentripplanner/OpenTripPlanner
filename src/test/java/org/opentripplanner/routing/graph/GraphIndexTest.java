package org.opentripplanner.routing.graph;

import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.GtfsTest;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStop;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

import java.util.List;

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

}
