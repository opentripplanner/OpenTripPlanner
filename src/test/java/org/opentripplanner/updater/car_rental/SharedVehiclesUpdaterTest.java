package org.opentripplanner.updater.car_rental;

import junit.framework.TestCase;
import org.apache.commons.lang3.tuple.Pair;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.core.vehicle_sharing.*;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.edgetype.rentedgetype.RentVehicleAnywhereEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.updater.vehicle_sharing.vehicles_positions.SharedVehiclesUpdater;

import java.util.List;

import static java.util.Collections.singletonList;

public class SharedVehiclesUpdaterTest extends TestCase {

    public void testProjectingVehicles() {
        float long1 = (float) -77.0;
        float long2 = (float) -77.0005;
        float long3 = (float) -77.001;
        float lat1 = (float) 38.0;
        float lat2 = (float) 38.0005;
        float lat3 = (float) -38.001;

        float vehLong1 = (float) (0.3 * long1 + 0.7 * long2);
        float vehLat1 = (float) (0.3 * lat1 + 0.7 * lat2);


        float vehLong2 = (float) (0.7 * long1 + 0.3 * long2);
        float vehLat2 = (float) (0.7 * lat1 + 0.3 * lat2);

//      We need to initialize graph.
        Graph graph = new Graph();
        StreetVertex v1 = new IntersectionVertex(graph, "v1", long1, lat1, "v1");
        StreetVertex v2 = new IntersectionVertex(graph, "v2", long2, lat2, "v2");
        StreetVertex v3 = new IntersectionVertex(graph, "v3", long3, lat3, "v3");

        @SuppressWarnings("unused")
        Edge walk = new StreetEdge(v1, v2, GeometryUtils.makeLineString(long1, lat1,
                long2, lat2), "e1", 87, StreetTraversalPermission.PEDESTRIAN, false);

        @SuppressWarnings("unused")
        Edge mustCar = new StreetEdge(v2, v3, GeometryUtils.makeLineString(long2, lat2,
                long3, lat3), "e2", 87, StreetTraversalPermission.CAR, false);

        @SuppressWarnings("unused")
        RentVehicleAnywhereEdge car1 = new RentVehicleAnywhereEdge(v1);
        RentVehicleAnywhereEdge car2 = new RentVehicleAnywhereEdge(v2);

        SharedVehiclesUpdater sharedVehiclesUpdater = new SharedVehiclesUpdater();

        try {
            sharedVehiclesUpdater.setup(graph);
        } catch (Exception e) {
            fail();
        }

//        One vehicle appeared.
        List<VehicleDescription> appeared = singletonList(new CarDescription("1", vehLong1, vehLat1, FuelType.ELECTRIC, Gearbox.AUTOMATIC, new Provider(2, "PANEK")));

        List<Pair<Vertex, VehicleDescription>> coordsToVertex = sharedVehiclesUpdater.coordsToVertex(appeared);
        List<Pair<RentVehicleAnywhereEdge, VehicleDescription>> appearedEdges = sharedVehiclesUpdater.prepareAppearedEdge(coordsToVertex);

        assertEquals(1, appearedEdges.size());
        assertEquals(car2, appearedEdges.get(0).getKey());


        appeared = singletonList(new CarDescription("2", vehLong2, vehLat2, FuelType.ELECTRIC, Gearbox.AUTOMATIC, new Provider(2, "PANEK")));

        coordsToVertex = sharedVehiclesUpdater.coordsToVertex(appeared);
        appearedEdges = sharedVehiclesUpdater.prepareAppearedEdge(coordsToVertex);

        assertEquals(1, appearedEdges.size());
        assertEquals(car1, appearedEdges.get(0).getKey());


    }
}
