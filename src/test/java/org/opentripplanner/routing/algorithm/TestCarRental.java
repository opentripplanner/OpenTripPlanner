package org.opentripplanner.routing.algorithm;

import junit.framework.TestCase;
import org.opentripplanner.api.parameter.QualifiedModeSet;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.edgetype.rentedgetype.RentCarAnywhereEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;

public class TestCarRental extends TestCase {
    public void testBasic() throws Exception {
        // generate a very simple graph
        Graph graph = new Graph();
        StreetVertex v1 = new IntersectionVertex(graph, "v1", -77.0492, 38.856, "v1");
        StreetVertex v2 = new IntersectionVertex(graph, "v2", -77.0492, 38.857, "v2");
        StreetVertex v3 = new IntersectionVertex(graph, "v3", -77.0492, 38.858, "v3");

        @SuppressWarnings("unused")
        Edge walk = new StreetEdge(v1, v2, GeometryUtils.makeLineString(-77.0492, 38.856,
                -77.0492, 38.857), "S. Crystal Dr", 87, StreetTraversalPermission.PEDESTRIAN, false);

        @SuppressWarnings("unused")
        Edge mustCar = new StreetEdge(v2, v3, GeometryUtils.makeLineString(-77.0492, 38.857,
                -77.0492, 38.858), "S. Crystal Dr", 87, StreetTraversalPermission.CAR, false);

        AStar aStar = new AStar();

        // it is impossible to get from v1 to v3 by walking
        RoutingRequest options = new RoutingRequest(new TraverseModeSet("WALK,TRANSIT"));
        options.setRoutingContext(graph, v1, v3);
        ShortestPathTree tree = aStar.getShortestPathTree(options);
        GraphPath path = tree.getPath(v3, false);
        assertNull(path);

        // or driving + walking (assuming pushing car is disallowed)
        options = new RoutingRequest(new TraverseModeSet("WALK,CAR,TRANSIT"));
        options.freezeTraverseMode();
        options.setRoutingContext(graph, v1, v3);
        tree = aStar.getShortestPathTree(options);
        path = tree.getPath(v3, false);
        assertNull(path);

        // so we add renting cars
        @SuppressWarnings("unused")
        RentCarAnywhereEdge car1 = new RentCarAnywhereEdge(v1, 1, 2);
        RentCarAnywhereEdge car2 = new RentCarAnywhereEdge(v2, 1, 2);

        // but the are no cars so we still fail
        options = new RoutingRequest();
        new QualifiedModeSet("CAR_RENT,TRANSIT").applyToRoutingRequest(options);
        options.setRoutingContext(graph, v1, v3);
        tree = aStar.getShortestPathTree(options);
        path = tree.getPath(v3, false);
        assertNull(path);

        // we add a car
        car2.available++;

        // but we can't park a car at v3, so we still fail
        options = new RoutingRequest();
        new QualifiedModeSet("CAR_RENT,TRANSIT").applyToRoutingRequest(options);
        options.setRoutingContext(graph, v1, v3);
        tree = aStar.getShortestPathTree(options);
        path = tree.getPath(v3, false);
        // null is returned because the only state at the target is not final
        assertNull(path);

        // we add a parking at v3
        @SuppressWarnings("unused")
        RentCarAnywhereEdge car3 = new RentCarAnywhereEdge(v3, 1, 2);

        // now we succeed!
        options = new RoutingRequest();
        new QualifiedModeSet("CAR_RENT,TRANSIT").applyToRoutingRequest(options);
        options.setRoutingContext(graph, v1, v3);
        tree = aStar.getShortestPathTree(options);
        path = tree.getPath(v3, false);
        assertNotNull(path);
    }
}
