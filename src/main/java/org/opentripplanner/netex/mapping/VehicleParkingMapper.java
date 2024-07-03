package org.opentripplanner.netex.mapping;

import static org.rutebanken.netex.model.ParkingVehicleEnumeration.CYCLE;
import static org.rutebanken.netex.model.ParkingVehicleEnumeration.E_CYCLE;
import static org.rutebanken.netex.model.ParkingVehicleEnumeration.PEDAL_CYCLE;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingSpaces;
import org.rutebanken.netex.model.Parking;
import org.rutebanken.netex.model.ParkingVehicleEnumeration;

/**
 * Maps from NeTEx Parking to an internal {@link VehicleParking}.
 */
class VehicleParkingMapper {

  private final FeedScopedIdFactory idFactory;

  private static final Set<ParkingVehicleEnumeration> BICYCLE_TYPES = Set.of(
    PEDAL_CYCLE,
    E_CYCLE,
    CYCLE
  );

  public VehicleParkingMapper(FeedScopedIdFactory idFactory) {
    this.idFactory = idFactory;
  }

  Collection<VehicleParking> map(Collection<Parking> parkings) {
    return parkings.stream().map(this::map).collect(Collectors.toUnmodifiableSet());
  }

  VehicleParking map(Parking parking) {
    return VehicleParking
      .builder()
      .id(idFactory.createId(parking.getId()))
      .name(NonLocalizedString.ofNullable(parking.getName().getValue()))
      .coordinate(WgsCoordinateMapper.mapToDomain(parking.getCentroid()))
      .capacity(mapCapacity(parking))
      .bicyclePlaces(hasBikes(parking))
      .carPlaces(!hasBikes(parking))
      .entrance(mapEntrance(parking))
      .build();
  }

  private VehicleParking.VehicleParkingEntranceCreator mapEntrance(Parking parking) {
    return builder ->
      builder
        .entranceId(idFactory.createId(parking.getId() + "/entrance"))
        .coordinate(WgsCoordinateMapper.mapToDomain(parking.getCentroid()))
        .walkAccessible(true)
        .carAccessible(true);
  }

  private static VehicleParkingSpaces mapCapacity(Parking parking) {
    var builder = VehicleParkingSpaces.builder();
    int capacity = parking.getTotalCapacity().intValue();

    // we assume that if we have bicycle in vehicle types it's a bicycle parking lot
    // it's not possible in NeTEx to split the spaces between the types, so if you want that
    // you have to define two parking lots with the same coordinates
    if (hasBikes(parking)) {
      builder.bicycleSpaces(capacity);
    } else {
      builder.carSpaces(capacity);
    }

    return builder.carSpaces(capacity).build();
  }

  private static boolean hasBikes(Parking parking) {
    return parking.getParkingVehicleTypes().stream().anyMatch(BICYCLE_TYPES::contains);
  }
}
