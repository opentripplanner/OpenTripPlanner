package org.opentripplanner.inspector.vector.vertex;

import static org.opentripplanner.inspector.vector.KeyValue.kColl;
import static org.opentripplanner.inspector.vector.KeyValue.kv;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.opentripplanner.apis.support.mapping.PropertyMapper;
import org.opentripplanner.inspector.vector.KeyValue;
import org.opentripplanner.service.vehicleparking.model.VehicleParking;
import org.opentripplanner.service.vehicleparking.model.VehicleParkingEntrance;
import org.opentripplanner.service.vehiclerental.street.VehicleRentalPlaceVertex;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.BarrierVertex;
import org.opentripplanner.street.model.vertex.VehicleParkingEntranceVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.utils.collection.ListUtils;

public class VertexPropertyMapper extends PropertyMapper<Vertex> {

  @Override
  protected Collection<KeyValue> map(Vertex input) {
    List<KeyValue> baseProps = List.of(
      kv("elevation", findElevationForVertex(input)),
      kv("class", input.getClass().getSimpleName()),
      kv("label", input.getLabel().toString())
    );
    List<KeyValue> properties =
      switch (input) {
        case BarrierVertex v -> List.of(kv("permission", v.getBarrierPermissions().toString()));
        case VehicleRentalPlaceVertex v -> List.of(kv("rentalId", v.getStation()));
        case VehicleParkingEntranceVertex v -> List.of(
          kv("parkingId", v.getVehicleParking().getId()),
          kColl("spacesFor", spacesFor(v.getVehicleParking())),
          kColl("traversalPermission", traversalPermissions(v.getParkingEntrance()))
        );
        default -> List.of();
      };
    return ListUtils.combine(baseProps, properties);
  }

  private Set<TraverseMode> spacesFor(VehicleParking vehicleParking) {
    var ret = new HashSet<TraverseMode>();
    if (vehicleParking.hasAnyCarPlaces()) {
      ret.add(TraverseMode.CAR);
    }
    if (vehicleParking.hasBicyclePlaces()) {
      ret.add(TraverseMode.BICYCLE);
    }
    return ret;
  }

  private Set<TraverseMode> traversalPermissions(VehicleParkingEntrance entrance) {
    var ret = new HashSet<TraverseMode>();
    if (entrance.isCarAccessible()) {
      ret.add(TraverseMode.CAR);
    }
    if (entrance.isWalkAccessible()) {
      ret.add(TraverseMode.WALK);
    }
    return ret;
  }

  private Double findElevationForVertex(Vertex v) {
    return Stream
      .concat(
        v
          .getIncomingStreetEdges()
          .stream()
          .filter(StreetEdge::hasElevationExtension)
          .map(streetEdge ->
            streetEdge
              .getElevationProfile()
              .getCoordinate(streetEdge.getElevationProfile().size() - 1)
              .y
          ),
        v
          .getOutgoingStreetEdges()
          .stream()
          .filter(StreetEdge::hasElevationExtension)
          .map(streetEdge -> streetEdge.getElevationProfile().getCoordinate(0).y)
      )
      .findAny()
      .orElse(null);
  }
}
