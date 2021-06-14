package org.opentripplanner.routing.vehicle_parking;

import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.util.NonLocalizedString;

public abstract class VehicleParkingTestBase {

  private static final String TEST_FEED_ID = "TEST";

  protected Graph graph;
  protected IntersectionVertex A, B;

  protected void initGraph() {
    graph = new Graph();
    graph.hasStreets = true;

    A = new IntersectionVertex(graph, "A", 0, 0);
    B = new IntersectionVertex(graph, "B", 0.01, 0);

    street(A, B, StreetTraversalPermission.PEDESTRIAN);
  }

  protected void street(StreetVertex from, StreetVertex to, StreetTraversalPermission permissions) {
    new StreetEdge(from, to,
        GeometryUtils.makeLineString(from.getLat(), from.getLon(), to.getLat(), to.getLon()),
        String.format("%s%s street", from.getName(), to.getName()),
        1,
        permissions,
        false
    );
  }

  protected VehicleParking createParkingWithEntrances(String id, double x, double y) {
    return createParkingWithEntrances(id, x, y, null);
  }

  protected VehicleParking createParkingWithEntrances(String id, double x, double y, VehicleSpaces vehiclePlaces) {
    VehicleParking.VehicleParkingEntranceCreator entrance = builder -> builder
        .entranceId(new FeedScopedId(TEST_FEED_ID, "Entrance " + id))
        .name(new NonLocalizedString("Entrance " + id))
        .x(x)
        .y(y)
        .walkAccessible(true);

    return VehicleParking
        .builder()
        .id(new FeedScopedId(TEST_FEED_ID, id))
        .bicyclePlaces(true)
        .capacity(vehiclePlaces)
        .availability(vehiclePlaces)
        .entrance(entrance)
        .build();
  }
}
