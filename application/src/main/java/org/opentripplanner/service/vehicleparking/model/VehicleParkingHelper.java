package org.opentripplanner.service.vehicleparking.model;

import java.util.List;
import java.util.Objects;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.edge.StreetVehicleParkingLink;
import org.opentripplanner.street.model.edge.VehicleParkingEdge;
import org.opentripplanner.street.model.vertex.VehicleParkingEntranceVertex;
import org.opentripplanner.street.model.vertex.VertexFactory;

public class VehicleParkingHelper {

  private final VertexFactory vertexFactory;

  public VehicleParkingHelper(Graph graph) {
    Objects.requireNonNull(graph);
    this.vertexFactory = new VertexFactory(graph);
  }

  public void linkVehicleParkingToGraph(VehicleParking vehicleParking) {
    var vehicleParkingVertices = createVehicleParkingVertices(vehicleParking);
    VehicleParkingHelper.linkVehicleParkingEntrances(vehicleParkingVertices);
  }

  public List<VehicleParkingEntranceVertex> createVehicleParkingVertices(
    VehicleParking vehicleParking
  ) {
    return vehicleParking
      .getEntrances()
      .stream()
      .map(vertexFactory::vehicleParkingEntrance)
      .toList();
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
}
