package org.opentripplanner.inspector.vector.vertex;

import static org.opentripplanner.inspector.vector.KeyValue.kColl;
import static org.opentripplanner.inspector.vector.KeyValue.kv;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.opentripplanner.apis.support.mapping.PropertyMapper;
import org.opentripplanner.framework.collection.ListUtils;
import org.opentripplanner.inspector.vector.KeyValue;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingEntrance;
import org.opentripplanner.service.vehiclerental.street.VehicleRentalPlaceVertex;
import org.opentripplanner.street.model.vertex.BarrierVertex;
import org.opentripplanner.street.model.vertex.VehicleParkingEntranceVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;

public class VertexPropertyMapper extends PropertyMapper<Vertex> {

  @Override
  protected Collection<KeyValue> map(Vertex input) {
    List<KeyValue> baseProps = List.of(
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
}
