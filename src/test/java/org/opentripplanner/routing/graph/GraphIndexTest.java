package org.opentripplanner.routing.graph;

import graphql.ExecutionResult;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Trip;
import org.opentripplanner.GtfsTest;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.vertextype.TransitStop;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;

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
        String feedId = graph.getFeedIds().iterator().next();
        Agency agency;
        agency = graph.index.agenciesForFeedId.get(feedId).get("azerty");
        assertNull(agency);
        agency = graph.index.agenciesForFeedId.get(feedId).get("agency");
        assertEquals(agency.getId(), "agency");
        assertEquals(agency.getName(), "Fake Agency");

        /* Stops */
        graph.index.stopForId.get(new FeedScopedId("X", "Y"));

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
        String feedId = graph.getFeedIds().iterator().next();
        Stop stopJ = graph.index.stopForId.get(new FeedScopedId(feedId, "J"));
        Stop stopL = graph.index.stopForId.get(new FeedScopedId(feedId, "L"));
        Stop stopM = graph.index.stopForId.get(new FeedScopedId(feedId, "M"));
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

    public void testGraphQLSimple() {
        String query =
                "query Agency{" +
                "    agency(id: \"agency\"){" +
                "        name" +
                "    }" +
                "}";

        ExecutionResult result = graph.index.graphQL.execute(query);
        assertTrue(result.getErrors().isEmpty());
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertEquals("Fake Agency", ((Map) data.get("agency")).get("name"));
    }

    public void testGraphQLNested() {
        String query =
                "query Agency{\n" +
                        "    viewer {" +
                        "    agency(id: \"agency\"){\n" +
                        "        name\n" +
                        "        routes{\n" +
                        "            shortName" +
                        "        }" +
                        "    }}\n" +
                        "}\n";

        ExecutionResult result = graph.index.graphQL.execute(query);
        assertTrue(result.getErrors().isEmpty());
        Map<String, Object> data = (Map<String, Object>) result.getData();
        assertEquals(18, ((List) ((Map) ((Map) data.get("viewer")).get("agency")).get("routes")).size());

    }

    public void testGraphQLIntrospectionQuery() {
        String query = "  query IntrospectionQuery {\n"
            + "    __schema {\n"
            + "      queryType { name }\n"
            + "      mutationType { name }\n"
            + "      types {\n"
            + "        ...FullType\n"
            + "      }\n"
            + "      directives {\n"
            + "        name\n"
            + "        description\n"
            + "        args {\n"
            + "          ...InputValue\n"
            + "        }\n"
            + "        onOperation\n"
            + "        onFragment\n"
            + "        onField\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "\n"
            + "  fragment FullType on __Type {\n"
            + "    kind\n"
            + "    name\n"
            + "    description\n"
            + "    fields {\n"
            + "      name\n"
            + "      description\n"
            + "      args {\n"
            + "        ...InputValue\n"
            + "      }\n"
            + "      type {\n"
            + "        ...TypeRef\n"
            + "      }\n"
            + "      isDeprecated\n"
            + "      deprecationReason\n"
            + "    }\n"
            + "    inputFields {\n"
            + "      ...InputValue\n"
            + "    }\n"
            + "    interfaces {\n"
            + "      ...TypeRef\n"
            + "    }\n"
            + "    enumValues {\n"
            + "      name\n"
            + "      description\n"
            + "      isDeprecated\n"
            + "      deprecationReason\n"
            + "    }\n"
            + "    possibleTypes {\n"
            + "      ...TypeRef\n"
            + "    }\n"
            + "  }\n"
            + "\n"
            + "  fragment InputValue on __InputValue {\n"
            + "    name\n"
            + "    description\n"
            + "    type { ...TypeRef }\n"
            + "    defaultValue\n"
            + "  }\n"
            + "\n"
            + "  fragment TypeRef on __Type {\n"
            + "    kind\n"
            + "    name\n"
            + "    ofType {\n"
            + "      kind\n"
            + "      name\n"
            + "      ofType {\n"
            + "        kind\n"
            + "        name\n"
            + "        ofType {\n"
            + "          kind\n"
            + "          name\n"
            + "        }\n"
            + "      }\n"
            + "    }\n"
            + "  }";

        ExecutionResult result = graph.index.graphQL.execute(query);
        assertTrue(result.getErrors().isEmpty());
    }


    public void testParentStations() {
        // graph.index.stopsForParentStation;
    }

    public void testLucene() {
        // graph.index.luceneIndex
    }

}
