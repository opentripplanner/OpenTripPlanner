package org.opentripplanner.street.linking;

import java.util.List;
import java.util.Objects;
import org.opentripplanner.service.vehicleparking.model.VehicleParking;
import org.opentripplanner.service.vehicleparking.model.VehicleParkingEntrance;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.edge.StreetVehicleParkingLink;
import org.opentripplanner.street.model.edge.VehicleParkingEdge;
import org.opentripplanner.street.model.vertex.VehicleParkingEntranceVertex;
import org.opentripplanner.street.model.vertex.Vertex;

public class VehicleParkingHelper {

  private final Graph graph;

  public VehicleParkingHelper(Graph graph) {
    this.graph = Objects.requireNonNull(graph);
  }

  public void linkVehicleParkingToGraph(VehicleParking vehicleParking) {
    var vehicleParkingVertices = createVehicleParkingVertices(vehicleParking);
    VehicleParkingHelper.linkVehicleParkingEntrances(vehicleParkingVertices);
  }

  public List<VehicleParkingEntranceVertex> createVehicleParkingVertices(
    VehicleParking vehicleParking
  ) {
    return vehicleParking.getEntrances().stream().map(this::vehicleParkingEntrance).toList();
  }

  public static void linkVehicleParkingEntrances(
    List<VehicleParkingEntranceVertex> vehicleParkingVertices
  ) {
    for (int i = 0; i < vehicleParkingVertices.size(); i++) {
      var currentVertex = vehicleParkingVertices.get(i);
      if (isUsableForParking(currentVertex, currentVertex)) {
        VehicleParkingEdge.createVehicleParkingEdge(currentVertex);
      }
      for (int j = i + 1; j < vehicleParkingVertices.size(); j++) {
        var nextVertex = vehicleParkingVertices.get(j);
        if (isUsableForParking(currentVertex, nextVertex)) {
          VehicleParkingEdge.createVehicleParkingEdge(currentVertex, nextVertex);
          VehicleParkingEdge.createVehicleParkingEdge(nextVertex, currentVertex);
        }
      }
    }
  }

  public static void linkToGraph(VehicleParkingEntranceVertex vehicleParkingEntrance) {
    StreetVehicleParkingLink.createStreetVehicleParkingLink(
      vehicleParkingEntrance,
      vehicleParkingEntrance.getParkingEntrance().getVertex()
    );
    StreetVehicleParkingLink.createStreetVehicleParkingLink(
      vehicleParkingEntrance.getParkingEntrance().getVertex(),
      vehicleParkingEntrance
    );
    vehicleParkingEntrance.getParkingEntrance().clearVertex();
  }

  public VehicleParkingEntranceVertex vehicleParkingEntrance(VehicleParkingEntrance entrance) {
    return addToGraph(new VehicleParkingEntranceVertex(entrance));
  }

  private static boolean isUsableForParking(
    VehicleParkingEntranceVertex from,
    VehicleParkingEntranceVertex to
  ) {
    var usableForBikeParking =
      from.getVehicleParking().hasBicyclePlaces() &&
      from.isWalkAccessible() &&
      to.isWalkAccessible();

    var usableForCarParking =
      from.getVehicleParking().hasAnyCarPlaces() &&
      ((from.isCarAccessible() && to.isWalkAccessible()) ||
        (from.isWalkAccessible() && to.isCarAccessible()));

    return usableForBikeParking || usableForCarParking;
  }

  private <T extends Vertex> T addToGraph(T vertex) {
    graph.addVertex(vertex);
    return vertex;
  }
}
