package org.opentripplanner.routing.vehicle_parking;

import java.util.List;
import java.util.stream.Collectors;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.edge.StreetVehicleParkingLink;
import org.opentripplanner.street.model.edge.VehicleParkingEdge;
import org.opentripplanner.street.model.vertex.VehicleParkingEntranceVertex;

public class VehicleParkingHelper {

  private VehicleParkingHelper() {}

  public static void linkVehicleParkingToGraph(Graph graph, VehicleParking vehicleParking) {
    var vehicleParkingVertices = VehicleParkingHelper.createVehicleParkingVertices(
      graph,
      vehicleParking
    );
    VehicleParkingHelper.linkVehicleParkingEntrances(vehicleParkingVertices);
  }

  public static List<VehicleParkingEntranceVertex> createVehicleParkingVertices(
    Graph graph,
    VehicleParking vehicleParking
  ) {
    return vehicleParking
      .getEntrances()
      .stream()
      .map(entrance -> new VehicleParkingEntranceVertex(graph, entrance))
      .collect(Collectors.toList());
  }

  public static void linkVehicleParkingEntrances(
    List<VehicleParkingEntranceVertex> vehicleParkingVertices
  ) {
    for (int i = 0; i < vehicleParkingVertices.size(); i++) {
      var currentVertex = vehicleParkingVertices.get(i);
      if (isUsableForParking(currentVertex, currentVertex)) {
        new VehicleParkingEdge(currentVertex);
      }
      for (int j = i + 1; j < vehicleParkingVertices.size(); j++) {
        var nextVertex = vehicleParkingVertices.get(j);
        if (isUsableForParking(currentVertex, nextVertex)) {
          new VehicleParkingEdge(currentVertex, nextVertex);
          new VehicleParkingEdge(nextVertex, currentVertex);
        }
      }
    }
  }

  public static void linkToGraph(VehicleParkingEntranceVertex vehicleParkingEntrance) {
    new StreetVehicleParkingLink(
      vehicleParkingEntrance,
      vehicleParkingEntrance.getParkingEntrance().getVertex()
    );
    new StreetVehicleParkingLink(
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
      (
        (from.isCarAccessible() && to.isWalkAccessible()) ||
        (from.isWalkAccessible() && to.isCarAccessible())
      );

    return usableForBikeParking || usableForCarParking;
  }
}
